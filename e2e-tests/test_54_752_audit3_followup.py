"""
v2 전수검사 3차 후속 E2E 테스트 (#752).

시나리오: Swagger 에서 @CurrentUserId(토큰 값)가 파라미터로 보이지 않고 query userId 를 보내도 무시 →
G-7a 보낸 사람의 선물 완료 상세 → Gift_400_5 문구(종료·준비중 공연) → 옵션 설명 50자(v2 생성·수정, v1 은 그대로) →
문의처 형식 Event_400_20 → 같은 무료 선착순 요청의 앞 주문이 확정 전이면 Order_400_26 → O-3 lines 에 선물 필드 없음.

DB 직접 접근(공연 상태·시각, 주문 상태 되돌리기)은 conftest 의 e2e_db fixture(환경변수 E2E_DB 등, #737)로 한다.
재실행해도 충돌하지 않도록 유저 이메일에 실행마다 다른 접미사를 붙인다.
"""
import base64
import json
import uuid
from datetime import datetime, timedelta
from urllib.parse import quote

import pytest
import requests

from conftest import assert_status, get_data

RUN = uuid.uuid4().hex[:8]
FMT = "%Y.%m.%d %H:%M"
START = (datetime.now() + timedelta(days=30)).replace(hour=18, minute=0, second=0, microsecond=0)
END = START + timedelta(minutes=120)
PLACE = {"name": "롤링홀", "address": "서울 마포구 어울마당로 35", "latitude": 37.548369, "longitude": 126.920036}
SECTIONS = [{"title": "공연 소개", "content": "<p>3차 후속</p>", "sortOrder": 0}]
PEOPLE = ["master", "manager", "guest", "sender", "receiver", "other", "buyer"]
TOKEN_PARAMS = {"userId", "currentUserId", "adminUserId"}


class State:
    tokens: dict = {}
    emails: dict = {}
    user_ids: dict = {}
    host_id: int = 0
    events: dict = {}
    tickets: dict = {}


@pytest.fixture(scope="module")
def s():
    return State()


DB = None


@pytest.fixture(autouse=True, scope="module")
def _bind_db(e2e_db):
    global DB
    DB = e2e_db


def _sql(sql):
    return DB.query(sql)


def _h(s, who):
    return {"Authorization": f"Bearer {s.tokens[who]}"}


def _code(resp):
    return resp.json().get("code")


def _login(base_url, email, name):
    resp = requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={"email": email, "name": name, "phoneNumber": "010-0000-0000", "profileImage": None, "marketingAgree": False},
    )
    assert_status(resp, 200)
    token = get_data(resp)["accessToken"]
    part = token.split(".")[1]
    part += "=" * (-len(part) % 4)
    return token, int(json.loads(base64.urlsafe_b64decode(part))["sub"])


def _ev(base_url, event_id, path=""):
    return f"{base_url}/v2/events/{event_id}{path}"


def _new_event(base_url, s, key):
    """무료 선착순(즉시 발급) 티켓 1개짜리 공개 공연"""
    resp = requests.post(
        f"{base_url}/v2/events",
        json={"hostId": s.host_id, "name": f"후속공연{key}", "startAt": START.strftime(FMT), "endAt": END.strftime(FMT), "hasTicket": True},
        headers=_h(s, "manager"),
    )
    assert_status(resp, 200)
    event_id = get_data(resp)["eventId"]
    resp = requests.post(_ev(base_url, event_id, "/ticket-items"), json={
        "payType": "FREE", "name": "무료", "description": "무료", "price": 0, "supplyCount": 100, "account": None,
        "approvalRequired": False, "isQuantityPublic": True, "purchaseLimit": 4, "saleStartAt": None, "saleEndAt": None,
    }, headers=_h(s, "manager"))
    assert_status(resp, 200)
    ticket_id = get_data(resp)["ticketItemId"]
    key_img = get_data(requests.post(_ev(base_url, event_id, "/images"), json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "manager")))["key"]
    assert_status(requests.patch(_ev(base_url, event_id, "/basic"), json={"posterImageKey": key_img, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]}, headers=_h(s, "manager")), 200)
    assert_status(requests.put(_ev(base_url, event_id, "/sections"), json=SECTIONS, headers=_h(s, "manager")), 200)
    assert_status(requests.post(_ev(base_url, event_id, "/open"), headers=_h(s, "manager")), 200)
    s.events[key], s.tickets[key] = event_id, ticket_id
    return event_id


def _order_body(s, key, quantity=1):
    return {
        "eventId": s.events[key], "ticketItemId": s.tickets[key], "quantity": quantity,
        "options": {"applyToAll": True, "answers": []}, "perTicketOptions": None,
        "paymentMethod": "FREE", "depositorName": None, "agreeRefundPolicy": True,
    }


def _buy(base_url, s, who, key, quantity=1):
    """무료 즉시 발급 주문 → (orderUuid, 티켓 uuid 목록)"""
    resp = requests.post(f"{base_url}/v2/orders", json=_order_body(s, key, quantity), headers=_h(s, who))
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["status"] == "APPROVED"
    return data["orderUuid"], [t["ticketUuid"] for t in data["issuedTickets"]]


def _gift_ok(base_url, s, who, ticket_uuid):
    resp = requests.post(f"{base_url}/v2/me/tickets/{ticket_uuid}/gift", json={"memo": None}, headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)


def _accept(base_url, s, who, token):
    return requests.post(f"{base_url}/v2/gifts/{token}/accept", headers=_h(s, who))


def _sent_ticket(base_url, s, who, gift_id):
    return requests.get(f"{base_url}/v2/me/gifts/{gift_id}/ticket", headers=_h(s, who) if who else {})


def _docs(base_url, s, group):
    """Swagger 문서 (서버 루트의 /v3/api-docs/{그룹}, 로그인 필요)"""
    root = base_url[: -len("/api")] if base_url.endswith("/api") else base_url
    resp = requests.get(f"{root}/v3/api-docs/{quote(group)}", headers=_h(s, "other"))
    assert_status(resp, 200)
    return resp.json()


def test_01_setup(base_url, s):
    for who in PEOPLE:
        email = f"audit752-{who}-{RUN}@dudoong.com"
        s.tokens[who], s.user_ids[who] = _login(base_url, email, f"후속{who}")
        s.emails[who] = email
    resp = requests.post(f"{base_url}/v2/hosts", json={"name": f"후속{RUN[:5]}", "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    s.host_id = get_data(resp)["hostId"]
    resp = requests.post(
        f"{base_url}/v2/hosts/{s.host_id}/members",
        json={"members": [{"email": s.emails["manager"], "role": "MANAGER"}, {"email": s.emails["guest"], "role": "GUEST"}]},
        headers=_h(s, "master"),
    )
    assert_status(resp, 200)
    for key in ("gift", "ended", "preparing", "order"):
        _new_event(base_url, s, key)


def test_02_swagger_hides_current_user_id(base_url, s):
    for group in ("v2-전체", "v2-호스팅센터", "v2-사용자앱", "v1", "internal"):
        docs = _docs(base_url, s, group)
        params = [p for item in docs["paths"].values() for op in item.values() if isinstance(op, dict) for p in op.get("parameters", [])]
        assert params, group
        leaked = [p for p in params if p.get("in") != "path" and p.get("name") in TOKEN_PARAMS]
        assert leaked == [], f"{group}: {leaked[:3]}"
    v2 = _docs(base_url, s, "v2-전체")
    # 진짜 경로 변수 userId 는 그대로 (H-10)
    role = v2["paths"]["/api/v2/hosts/{hostId}/members/{userId}/role"]["patch"]["parameters"]
    assert any(p["name"] == "userId" and p["in"] == "path" for p in role)
    # T-1 sort 설명 (#752)
    t1 = v2["paths"]["/api/v2/me/tickets"]["get"]["parameters"]
    assert [p["name"] for p in t1] == ["sort"] and "UPCOMING" in t1[0]["description"]


def test_03_query_user_id_is_ignored(base_url, s):
    resp = requests.get(f"{base_url}/v2/me", params={"userId": s.user_ids["other"]}, headers=_h(s, "buyer"))
    assert_status(resp, 200)
    assert get_data(resp)["userId"] == s.user_ids["buyer"]
    resp = requests.get(f"{base_url}/v2/me", params={"userId": s.user_ids["other"]})
    assert resp.status_code == 401


def test_04_g7a_sent_gift_ticket(base_url, s):
    order_uuid, uuids = _buy(base_url, s, "sender", "gift", 2)
    created = _gift_ok(base_url, s, "sender", uuids[0])
    gift_id, token = created["giftId"], created["giftToken"]
    # 대기 중에는 선물 완료가 아니라 404
    assert _code(_sent_ticket(base_url, s, "sender", gift_id)) == "Gift_404_1"
    assert_status(_accept(base_url, s, "receiver", token), 200)

    resp = _sent_ticket(base_url, s, "sender", gift_id)
    assert_status(resp, 200)
    detail = get_data(resp)
    assert detail["giftState"] == "SENT" and detail["state"] == "GIFT_SENT"
    assert detail["ticketUuid"] is None and detail["qrValue"] is None
    assert detail["issuedTicketNo"].startswith("T") and detail["orderUuid"] == order_uuid
    assert detail["gift"]["receiverName"] and detail["canGift"] is False and detail["canReturn"] is False
    # T-1 의 선물 완료 행과 같은 티켓
    groups = requests.get(f"{base_url}/v2/me/tickets", headers=_h(s, "sender")).json()["data"]["groups"]
    row = next(t for g in groups for t in g["tickets"] if t.get("giftId") == gift_id)
    assert row["issuedTicketNo"] == detail["issuedTicketNo"]
    # 받은 사람·남·없는 id 는 404, 비로그인 401
    assert _code(_sent_ticket(base_url, s, "receiver", gift_id)) == "Gift_404_1"
    assert _code(_sent_ticket(base_url, s, "other", gift_id)) == "Gift_404_1"
    assert _code(_sent_ticket(base_url, s, "sender", 987654321)) == "Gift_404_1"
    assert _sent_ticket(base_url, s, None, gift_id).status_code == 401
    # 받은 사람이 반환하면 더 이상 선물 완료가 아니다
    received = requests.get(f"{base_url}/v2/me/tickets", headers=_h(s, "receiver")).json()["data"]["groups"]
    receiver_uuid = next(t["ticketUuid"] for g in received for t in g["tickets"] if t["giftState"] == "RECEIVED")
    assert_status(requests.post(f"{base_url}/v2/me/tickets/{receiver_uuid}/return", headers=_h(s, "receiver")), 200)
    assert _code(_sent_ticket(base_url, s, "sender", gift_id)) == "Gift_404_1"


def test_05_gift_400_5_message(base_url, s):
    tokens = []
    for key in ("ended", "preparing"):
        _, uuids = _buy(base_url, s, "sender", key)
        tokens.append(_gift_ok(base_url, s, "sender", uuids[0])["giftToken"])
    _sql(f"UPDATE tbl_event SET start_at = NOW() - INTERVAL 3 HOUR WHERE event_id = {s.events['ended']}")
    # 연쇄 처리 없이 상태만 바꿔 대기 선물을 남긴다 (운영 준비중 전환은 대기 선물을 취소)
    _sql(f"UPDATE tbl_event SET status = 'PREPARING' WHERE event_id = {s.events['preparing']}")
    for token in tokens:
        resp = _accept(base_url, s, "other", token)
        assert resp.status_code == 400
        assert resp.json()["code"] == "Gift_400_5" and resp.json()["reason"] == "선물을 받을 수 없는 공연입니다."
        landing = requests.get(f"{base_url}/v2/gifts/{token}", headers=_h(s, "other"))
        assert get_data(landing)["viewState"] == "EXPIRED"


def test_06_option_description_50(base_url, s):
    ev = s.events["order"]

    def v2_create(description):
        return requests.post(_ev(base_url, ev, "/options"), json={"name": "옵션", "description": description, "type": "YES_NO"}, headers=_h(s, "manager"))

    resp = v2_create("가" * 50)
    assert_status(resp, 200)
    assert get_data(resp)["description"] == "가" * 50
    assert v2_create("가" * 51).status_code == 400

    # v1 옵션 API 는 그대로 (길이 제한 없음)
    long = "다" * 60
    resp = requests.post(f"{base_url}/v1/events/{ev}/ticketOptions", json={"type": "Y/N", "name": "v1옵션", "description": long, "additionalPrice": 0}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    option_id = get_data(resp)["optionGroupId"]

    def patch(body):
        return requests.patch(_ev(base_url, ev, f"/options/{option_id}"), json=body, headers=_h(s, "manager"))

    # 설명을 그대로 다시 보내면 길어도 통과 (폼 전체 재전송)
    resp = patch({"name": "새이름", "description": f" {long} "})
    assert_status(resp, 200)
    assert get_data(resp)["name"] == "새이름" and get_data(resp)["description"] == long
    resp = patch({"description": long + "라"})
    assert resp.status_code == 400 and _code(resp) == "Option_Group_400_6"
    assert_status(patch({"description": "짧은 설명"}), 200)
    assert patch({"description": "나" * 51}).status_code == 400


def test_07_event_contact_400_20(base_url, s):
    ev = s.events["order"]

    def basic(contacts):
        return requests.patch(_ev(base_url, ev, "/basic"), json={"contacts": contacts}, headers=_h(s, "manager"))

    # 요청 검증을 지나 도메인에서 걸리는 형식 오류: null 원소, 전각 공백만 (요청 검증은 Java trim 이라 통과, 저장 전 trim 하면 빈 값)
    for contacts in ([None], [{"type": "EMAIL", "value": "\u3000"}]):
        resp = basic(contacts)
        assert resp.status_code == 400 and _code(resp) == "Event_400_20", (contacts, resp.text[:200])
    # 공백·201자·11개는 요청 검증에서 400
    for contacts in (
        [{"type": "EMAIL", "value": " "}],
        [{"type": "EMAIL", "value": "a" * 201}],
        [{"type": "EMAIL", "value": f"{i}@a.com"} for i in range(11)],
    ):
        assert basic(contacts).status_code == 400
    # 실패한 요청은 반영되지 않는다
    manage = get_data(requests.get(_ev(base_url, ev, "/manage"), headers=_h(s, "guest")))
    assert [c["value"] for c in manage["contacts"]] == ["a@a.com"]
    assert_status(basic([{"type": "EMAIL", "value": "b@a.com"}]), 200)


def test_08_duplicate_order_in_progress_400_26(base_url, s):
    order_uuid, _ = _buy(base_url, s, "buyer", "order")
    # 앞 요청이 확정(발급) 중인 상태: 생성 직후 상태로 되돌림
    _sql(f"UPDATE tbl_order SET order_status = 'PENDING_PAYMENT' WHERE uuid = '{order_uuid}'")
    count = int(_sql(f"SELECT COUNT(*) FROM tbl_order WHERE user_id = {s.user_ids['buyer']}").strip())
    resp = requests.post(f"{base_url}/v2/orders", json=_order_body(s, "order"), headers=_h(s, "buyer"))
    assert resp.status_code == 400 and _code(resp) == "Order_400_26"
    assert int(_sql(f"SELECT COUNT(*) FROM tbl_order WHERE user_id = {s.user_ids['buyer']}").strip()) == count
    _sql(f"UPDATE tbl_order SET order_status = 'APPROVED' WHERE uuid = '{order_uuid}'")


def test_09_o3_lines_have_no_gift_fields(base_url, s):
    order_uuid, uuids = _buy(base_url, s, "sender", "order", 2)
    _gift_ok(base_url, s, "sender", uuids[0])
    detail = get_data(requests.get(f"{base_url}/v2/me/orders/{order_uuid}", headers=_h(s, "sender")))
    assert detail["lines"] and all(not ({"giftState", "isGiftExpired", "giftId"} & set(line)) for line in detail["lines"])
    assert sorted(t["giftState"] for t in detail["issuedTickets"]) == ["NONE", "PENDING"]
