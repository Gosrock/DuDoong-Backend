"""
v2 공연 운영 API E2E 테스트 (#712).

시나리오: 호스트·멤버·공연·티켓(옵션) 준비 → v2 등록 → v1 사용자 주문 여러 건 →
주문 목록·상태별 건수 → 승인 / 거절(사유 검증) / 취소 / v1 거절 분류 → 권한 경계·IDOR →
대시보드 → 발급 티켓 목록·상세 → 호스트 스캔(결과 4종) → 셀프 체크인(QR 토큰) → 환불 완료 → 엑셀 다운로드 → v1 호환.

재실행해도 충돌하지 않도록 유저 이메일에 실행마다 다른 접미사를 붙인다. DB 직접 접근은 하지 않는다.
엑셀은 표준 라이브러리(zipfile)로 xlsx 를 열어 헤더·행 수를 확인한다.
"""
import io
import re
import uuid
import zipfile
from datetime import datetime, timedelta
from xml.etree import ElementTree

import pytest
import requests

from conftest import assert_status, get_data

RUN = uuid.uuid4().hex[:8]
FMT = "%Y.%m.%d %H:%M"
START = (datetime.now() + timedelta(days=30)).replace(hour=18, minute=0, second=0, microsecond=0)
END = START + timedelta(minutes=120)
PLACE = {"name": "롤링홀", "address": "서울 마포구 어울마당로 35", "latitude": 37.548369, "longitude": 126.920036}
SECTIONS = [{"title": "공연 소개", "content": "<p>운영 테스트</p>", "sortOrder": 0}]
ACCOUNT = {"bank": "신한은행", "holder": "고스락", "number": "110-123-456789"}
BUYERS = ["approved", "refused", "v1refused", "canceled", "pending", "self", "multi"]


class OpState:
    tokens: dict = {}
    emails: dict = {}
    host_id: int = 0
    other_host_id: int = 0
    event_id: int = 0
    other_event_id: int = 0
    ticket_id: int = 0
    other_ticket_id: int = 0
    orders: dict = {}
    other_order: str = ""
    check_in_token: str = ""


@pytest.fixture(scope="module")
def s():
    return OpState()


def _f(dt):
    return dt.strftime(FMT)


def _h(s, who):
    return {"Authorization": f"Bearer {s.tokens[who]}"}


def _code(resp):
    return resp.json().get("code")


def _login(base_url, email, name, phone="010-0000-0000"):
    resp = requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={"email": email, "name": name, "phoneNumber": phone, "profileImage": None, "marketingAgree": False},
    )
    assert_status(resp, 200)
    return get_data(resp)["accessToken"]


def _ev(base_url, s, path, event_id=None):
    return f"{base_url}/v2/events/{event_id or s.event_id}{path}"


def _v1_order(base_url, s, who, ticket_id, event_id=None, quantity=1):
    event_id = event_id or s.event_id
    resp = requests.get(f"{base_url}/v1/events/{event_id}/ticketItems/{ticket_id}/options", headers=_h(s, who))
    assert_status(resp, 200)
    answers = []
    for group in get_data(resp)["optionGroups"]:
        rows = group["options"]
        if len(rows) == 1:
            answers.append({"optionId": rows[0]["optionId"], "answer": "홍길동"})
        else:
            answers.append({"optionId": next(r for r in rows if r["answer"] == "예")["optionId"], "answer": "예"})
    cart = requests.post(
        f"{base_url}/v1/carts", json={"items": [{"itemId": ticket_id, "quantity": quantity, "options": answers}]}, headers=_h(s, who),
    )
    assert cart.status_code in (200, 201), cart.text[:300]
    resp = requests.post(f"{base_url}/v1/orders/", json={"cartId": get_data(cart)["cartId"], "couponId": None}, headers=_h(s, who))
    assert resp.status_code in (200, 201), resp.text[:300]
    return get_data(resp)["orderId"]


def _orders(base_url, s, who="guest", **params):
    resp = requests.get(_ev(base_url, s, "/orders"), params=params, headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)


def _tickets_of(base_url, s, order_uuid):
    resp = requests.get(_ev(base_url, s, f"/orders/{order_uuid}"), headers=_h(s, "guest"))
    assert_status(resp, 200)
    return [t["ticketUuid"] for t in get_data(resp)["issuedTickets"]]


def _scan(base_url, s, ticket_uuid, who="guest"):
    resp = requests.post(_ev(base_url, s, "/check-ins"), json={"ticketUuid": ticket_uuid}, headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)


def _self(base_url, s, who, token, ticket_uuid=None):
    return requests.post(f"{base_url}/v2/check-ins/self", json={"token": token, "ticketUuid": ticket_uuid}, headers=_h(s, who))


def _xlsx_rows(content):
    """첫 시트의 행 목록 (각 행은 셀 문자열 리스트). POI 문자열은 sharedStrings, 숫자는 값 그대로"""
    with zipfile.ZipFile(io.BytesIO(content)) as z:
        ns = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
        shared = []
        if "xl/sharedStrings.xml" in z.namelist():
            root = ElementTree.fromstring(z.read("xl/sharedStrings.xml"))
            shared = ["".join(t.text or "" for t in si.iter(f"{{{ns['m']}}}t")) for si in root.findall("m:si", ns)]
        sheet = ElementTree.fromstring(z.read("xl/worksheets/sheet1.xml"))
        rows = []
        for row in sheet.iter(f"{{{ns['m']}}}row"):
            cells = []
            for c in row.findall("m:c", ns):
                col = re.match(r"[A-Z]+", c.get("r")).group(0)
                index = 0
                for ch in col:
                    index = index * 26 + (ord(ch) - 64)
                while len(cells) < index - 1:
                    cells.append("")
                v = c.find("m:v", ns)
                value = "" if v is None else v.text
                cells.append(shared[int(value)] if c.get("t") == "s" and value != "" else value)
            rows.append(cells)
        return rows


def test_01_setup(base_url, s):
    people = [("master", "운영마스터"), ("manager", "운영매니저"), ("guest", "운영일반"), ("outsider", "운영외부"), ("other", "운영남호스트")]
    people += [(b, f"구매{i}") for i, b in enumerate(BUYERS)]
    for who, name in people:
        email = f"v2op-{who}-{RUN}@dudoong.com"
        phone = "010-4242-0001" if who == "approved" else "010-0000-0000"
        s.tokens[who], s.emails[who] = _login(base_url, email, name, phone), email

    for who, attr, host_name in [("master", "host_id", f"운영{RUN[:5]}"), ("other", "other_host_id", f"남운영{RUN[:4]}")]:
        resp = requests.post(f"{base_url}/v2/hosts", json={"name": host_name, "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, who))
        assert_status(resp, 200)
        setattr(s, attr, get_data(resp)["hostId"])
    resp = requests.post(
        f"{base_url}/v2/hosts/{s.host_id}/members",
        json={"members": [{"email": s.emails["manager"], "role": "MANAGER"}, {"email": s.emails["guest"], "role": "GUEST"}]},
        headers=_h(s, "master"),
    )
    assert_status(resp, 200)

    for who, host_attr, event_attr, ticket_attr in [("manager", "host_id", "event_id", "ticket_id"), ("other", "other_host_id", "other_event_id", "other_ticket_id")]:
        resp = requests.post(
            f"{base_url}/v2/events",
            json={"hostId": getattr(s, host_attr), "name": "v2운영공연", "startAt": _f(START), "endAt": _f(END), "hasTicket": True},
            headers=_h(s, who),
        )
        assert_status(resp, 200)
        event_id = get_data(resp)["eventId"]
        setattr(s, event_attr, event_id)
        resp = requests.post(
            f"{base_url}/v2/events/{event_id}/ticket-items",
            json={"payType": "DUDOONG", "name": "일반", "description": "일반 입장", "price": 6000, "supplyCount": 30, "account": ACCOUNT,
                  "approvalRequired": True, "isQuantityPublic": True, "purchaseLimit": 4, "saleStartAt": None, "saleEndAt": None},
            headers=_h(s, who),
        )
        assert_status(resp, 200)
        setattr(s, ticket_attr, get_data(resp)["ticketItemId"])

    # 옵션 2개 (네/아니오 +1000, 주관식) 를 우리 티켓에 적용
    option_ids = []
    for body in [{"name": "뒷풀이", "description": "참석?", "type": "YES_NO", "yesAdditionalPrice": 1000}, {"name": "입금자명", "description": "입금자명", "type": "SUBJECTIVE"}]:
        resp = requests.post(_ev(base_url, s, "/options"), json=body, headers=_h(s, "manager"))
        assert_status(resp, 200)
        option_ids.append(get_data(resp)["optionId"])
    assert_status(requests.put(_ev(base_url, s, f"/ticket-items/{s.ticket_id}/options"), json={"optionIds": option_ids}, headers=_h(s, "manager")), 200)

    for who, event_id in [("manager", s.event_id), ("other", s.other_event_id)]:
        key = get_data(requests.post(f"{base_url}/v2/events/{event_id}/images", json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, who)))["key"]
        resp = requests.patch(
            f"{base_url}/v2/events/{event_id}/basic",
            json={"posterImageKey": key, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]},
            headers=_h(s, who),
        )
        assert_status(resp, 200)
        assert_status(requests.put(f"{base_url}/v2/events/{event_id}/sections", json=SECTIONS, headers=_h(s, who)), 200)
        assert_status(requests.post(f"{base_url}/v2/events/{event_id}/open", headers=_h(s, who)), 200)


def test_02_v1_orders(base_url, s):
    quantities = {"approved": 2, "multi": 2}
    for who in BUYERS:
        s.orders[who] = _v1_order(base_url, s, who, s.ticket_id, quantity=quantities.get(who, 1))
    s.other_order = _v1_order(base_url, s, "pending", s.other_ticket_id, event_id=s.other_event_id)

    data = _orders(base_url, s)
    assert data["counts"]["all"] == len(BUYERS) and data["counts"]["pendingApprove"] == len(BUYERS)
    assert {o["status"] for o in data["orders"]["content"]} == {"PENDING_APPROVE"}
    # 결제금액 = (6000 + 옵션 '예' 1000) x 매수
    approved = next(o for o in data["orders"]["content"] if o["orderUuid"] == s.orders["approved"])
    assert approved["totalQuantity"] == 2 and approved["totalPaymentAmount"] == 14000
    assert approved["buyerPhone"] == "010-4242-0001"


def test_03_approve_refuse_cancel(base_url, s):
    o = s.orders
    # 권한: 일반 멤버·외부인 403, 비로그인 401
    for who in ("guest", "outsider"):
        assert_status(requests.post(_ev(base_url, s, f"/orders/{o['approved']}/approve"), headers=_h(s, who)), 403)
    assert_status(requests.post(_ev(base_url, s, f"/orders/{o['approved']}/approve")), 401)

    for who in ("approved", "canceled", "self", "multi"):
        resp = requests.post(_ev(base_url, s, f"/orders/{o[who]}/approve"), headers=_h(s, "manager"))
        assert_status(resp, 200)
        assert get_data(resp)["order"]["status"] == "APPROVED"
    resp = requests.post(_ev(base_url, s, f"/orders/{o['approved']}/approve"), headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert _code(resp) == "Order_400_3"

    # 거절 사유 검증
    url = _ev(base_url, s, f"/orders/{o['refused']}/refuse")
    for body in [{"reasonType": "ETC"}, {"reasonType": "ETC", "reasonText": "가" * 21}]:
        resp = requests.post(url, json=body, headers=_h(s, "manager"))
        assert_status(resp, 400)
        assert _code(resp) == "Order_400_18"
    assert_status(requests.post(url, json={}, headers=_h(s, "manager")), 400)
    resp = requests.post(url, json={"reasonType": "ETC", "reasonText": "중복 주문"}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    order = get_data(resp)["order"]
    assert order["status"] == "REFUSED" and order["refuseReasonType"] == "ETC" and order["refuseReason"] == "중복 주문"
    assert order["refundStatus"] == "REQUESTED"

    # v1 API 거절 → v2 에서 REFUSED (사유 종류 없음)
    assert_status(requests.post(f"{base_url}/v1/events/{s.event_id}/orders/{o['v1refused']}/refuse", json={"reason": "v1 거절"}, headers=_h(s, "master")), 200)

    # 승인 완료 주문 취소
    resp = requests.post(_ev(base_url, s, f"/orders/{o['canceled']}/cancel"), json={"reason": "일정 변경"}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["order"]["status"] == "CANCELED" and data["order"]["cancelReason"] == "일정 변경"
    assert {t["entrance"] for t in data["issuedTickets"]} == {"CANCELED"}

    data = _orders(base_url, s)
    assert data["counts"] == {"all": 7, "pendingApprove": 1, "approved": 3, "refused": 2, "canceled": 1, "failed": 0}
    refused = _orders(base_url, s, status="REFUSED")["orders"]["content"]
    assert {r["orderUuid"]: r["refuseReasonType"] for r in refused} == {o["refused"]: "ETC", o["v1refused"]: None}
    assert next(r for r in refused if r["orderUuid"] == o["v1refused"])["refuseReason"] == "v1 거절"
    assert [r["orderUuid"] for r in _orders(base_url, s, searchType="PHONE", keyword="4242-0001")["orders"]["content"]] == [o["approved"]]


def test_04_idor_and_permissions(base_url, s):
    # 다른 공연 주문을 내 공연 경로로 → 404, 다른 호스트 공연 경로 → 403
    resp = requests.post(_ev(base_url, s, f"/orders/{s.other_order}/approve"), headers=_h(s, "manager"))
    assert_status(resp, 404)
    assert _code(resp) == "Order_404_1"
    assert_status(requests.get(_ev(base_url, s, f"/orders/{s.other_order}"), headers=_h(s, "manager")), 404)
    assert_status(requests.post(_ev(base_url, s, f"/orders/{s.other_order}/refuse"), json={"reasonType": "SOLD_OUT"}, headers=_h(s, "manager")), 404)
    assert_status(requests.post(_ev(base_url, s, f"/orders/{s.other_order}/approve", s.other_event_id), headers=_h(s, "manager")), 403)
    assert_status(requests.get(_ev(base_url, s, "/orders", s.other_event_id), headers=_h(s, "manager")), 403)
    other = requests.get(_ev(base_url, s, "/orders", s.other_event_id), headers=_h(s, "other"))
    assert_status(other, 200)
    assert get_data(other)["orders"]["content"][0]["status"] == "PENDING_APPROVE"
    for path in ("/orders", "/dashboard", "/issued-tickets", "/check-ins/stats", "/refunds"):
        assert_status(requests.get(_ev(base_url, s, path), headers=_h(s, "outsider")), 403)
        assert_status(requests.get(_ev(base_url, s, path), headers=_h(s, "guest")), 200)


def test_05_dashboard(base_url, s):
    resp = requests.get(_ev(base_url, s, "/dashboard"), headers=_h(s, "guest"))
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["orders"] == {"pendingApprove": 1, "approved": 3, "refused": 2, "refundRequested": 3}
    # 승인 3건(2+1+2장), 취소분은 재고 복구
    assert data["tickets"]["totalSoldCount"] == 5 and data["tickets"]["totalSupplyCount"] == 30
    assert data["tickets"]["items"][0]["soldCount"] == 5
    assert data["salesAmount"] == 7000 * 5
    assert data["entrance"] == {"issuedCount": 5, "enteredCount": 0, "notEnteredCount": 5, "entranceRate": 0.0}


def test_06_issued_tickets(base_url, s):
    resp = requests.get(_ev(base_url, s, "/issued-tickets"), headers=_h(s, "guest"))
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["counts"]["issuedCount"] == 5 and data["tickets"]["totalElements"] == 5
    first = data["tickets"]["content"][0]
    assert first["entrance"] == "BEFORE" and first["payType"] == "DUDOONG" and first["orderNo"].startswith("R")
    resp = requests.get(_ev(base_url, s, f"/issued-tickets/{first['ticketUuid']}"), headers=_h(s, "guest"))
    assert_status(resp, 200)
    answers = {a["optionName"]: a["answer"] for a in get_data(resp)["optionAnswers"]}
    assert answers == {"뒷풀이": "예", "입금자명": "홍길동"}


def test_07_host_scan(base_url, s):
    ticket = _tickets_of(base_url, s, s.orders["approved"])[0]
    result = _scan(base_url, s, ticket)
    assert result["result"] == "ENTERED" and result["ticket"]["ticketUuid"] == ticket
    assert _scan(base_url, s, ticket, who="manager")["result"] == "ALREADY_ENTERED"
    canceled_ticket = _tickets_of(base_url, s, s.orders["canceled"])[0]
    assert _scan(base_url, s, canceled_ticket)["result"] == "CANCELED"
    assert_status(requests.post(_ev(base_url, s, f"/orders/{s.other_order}/approve", s.other_event_id), headers=_h(s, "other")), 200)
    other_ticket = get_data(requests.get(_ev(base_url, s, f"/orders/{s.other_order}", s.other_event_id), headers=_h(s, "other")))["issuedTickets"][0]["ticketUuid"]
    for uuid_ in (other_ticket, str(uuid.uuid4())):
        result = _scan(base_url, s, uuid_)
        assert result["result"] == "OTHER_EVENT" and result["ticket"] is None
    assert_status(requests.post(_ev(base_url, s, "/check-ins"), json={"ticketUuid": ticket}, headers=_h(s, "outsider")), 403)

    # v1 발급 티켓 목록에도 입장 완료, v1 입장 API 는 이미 입장 400
    resp = requests.get(f"{base_url}/v1/events/{s.event_id}/issuedTickets", headers=_h(s, "master"))
    assert_status(resp, 200)
    assert next(t for t in get_data(resp)["content"] if t["uuid"] == ticket)["issuedTicketStatus"] == "입장 완료"
    resp = requests.patch(f"{base_url}/v1/events/{s.event_id}/issuedTickets/{ticket}", headers=_h(s, "master"))
    assert_status(resp, 400)
    assert _code(resp) == "IssuedTicket_400_5"

    resp = requests.get(_ev(base_url, s, "/check-ins/stats"), headers=_h(s, "guest"))
    assert_status(resp, 200)
    assert get_data(resp) == {"issuedCount": 5, "enteredCount": 1, "notEnteredCount": 4, "entranceRate": 20.0}


def test_08_self_check_in(base_url, s):
    resp = requests.get(_ev(base_url, s, "/check-in-qr"), headers=_h(s, "guest"))
    assert_status(resp, 200)
    data = get_data(resp)
    s.check_in_token = data["token"]
    assert len(s.check_in_token) == 43 and data["qrPath"] == f"/check-in?token={s.check_in_token}"
    assert get_data(requests.get(_ev(base_url, s, "/check-in-qr"), headers=_h(s, "master")))["token"] == s.check_in_token
    assert_status(requests.get(_ev(base_url, s, "/check-in-qr"), headers=_h(s, "outsider")), 403)

    resp = _self(base_url, s, "self", s.check_in_token)
    assert_status(resp, 200)
    assert get_data(resp)["result"] == "ENTERED"
    assert get_data(_self(base_url, s, "self", s.check_in_token))["result"] == "ALREADY_ENTERED"

    resp = _self(base_url, s, "multi", s.check_in_token)
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["result"] == "SELECT_TICKET" and len(data["candidates"]) == 2
    chosen = data["candidates"][1]["ticketUuid"]
    assert get_data(_self(base_url, s, "multi", s.check_in_token, chosen))["result"] == "ENTERED"

    # 남의 티켓 400, 티켓 없음 OTHER_EVENT, 다른 공연 토큰 OTHER_EVENT, 잘못된 토큰 400, 비로그인 401
    resp = _self(base_url, s, "pending", s.check_in_token, chosen)
    assert_status(resp, 400)
    assert _code(resp) == "IssuedTicket_400_1"
    assert get_data(_self(base_url, s, "outsider", s.check_in_token))["result"] == "OTHER_EVENT"
    other_token = get_data(requests.get(_ev(base_url, s, "/check-in-qr", s.other_event_id), headers=_h(s, "other")))["token"]
    assert other_token != s.check_in_token
    assert get_data(_self(base_url, s, "multi", other_token))["result"] == "OTHER_EVENT"
    resp = _self(base_url, s, "multi", "invalid-token")
    assert_status(resp, 400)
    assert _code(resp) == "Event_400_26"
    assert_status(requests.post(f"{base_url}/v2/check-ins/self", json={"token": s.check_in_token}), 401)
    assert get_data(_self(base_url, s, "canceled", s.check_in_token))["result"] == "CANCELED"

    resp = requests.get(_ev(base_url, s, "/check-ins/stats"), headers=_h(s, "guest"))
    assert get_data(resp)["enteredCount"] == 3


def test_09_refunds(base_url, s):
    resp = requests.get(_ev(base_url, s, "/refunds"), params={"status": "REQUESTED"}, headers=_h(s, "guest"))
    assert_status(resp, 200)
    requested = {r["orderUuid"]: r for r in get_data(resp)["content"]}
    assert set(requested) == {s.orders["refused"], s.orders["v1refused"], s.orders["canceled"]}
    assert requested[s.orders["refused"]]["reason"] == "중복 주문"

    url = _ev(base_url, s, f"/refunds/{s.orders['refused']}/complete")
    assert_status(requests.post(url, headers=_h(s, "guest")), 403)
    resp = requests.post(url, headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert get_data(resp)["order"]["refundStatus"] == "COMPLETED"
    assert_status(requests.post(url, headers=_h(s, "manager")), 200)
    resp = requests.post(_ev(base_url, s, f"/refunds/{s.orders['approved']}/complete"), headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert _code(resp) == "Order_400_17"

    # v1 환불 목록에서도 사유·완료 상태가 보인다
    resp = requests.get(f"{base_url}/v1/events/{s.event_id}/refunds", headers=_h(s, "master"))
    assert_status(resp, 200)
    v1 = {r["orderId"]: r for r in get_data(resp)["content"]}
    assert v1[s.orders["refused"]]["cancelReason"] == "중복 주문" and v1[s.orders["refused"]]["refundStatus"] == "REFUND_COMPLETED"
    assert get_data(requests.get(_ev(base_url, s, "/dashboard"), headers=_h(s, "guest")))["orders"]["refundRequested"] == 2


def test_10_excel(base_url, s):
    resp = requests.get(_ev(base_url, s, "/orders/export"), headers=_h(s, "guest"))
    assert_status(resp, 200)
    assert resp.headers["Content-Type"].startswith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    assert f"orders-{s.event_id}.xlsx" in resp.headers["Content-Disposition"]
    rows = _xlsx_rows(resp.content)
    assert rows[0] == ["주문번호", "주문자", "연락처", "이메일", "티켓", "매수", "결제금액", "주문일시", "상태", "환불", "거절·취소 사유"]
    assert len(rows) - 1 == 7
    rows = _xlsx_rows(requests.get(_ev(base_url, s, "/orders/export"), params={"status": "REFUSED"}, headers=_h(s, "guest")).content)
    assert len(rows) - 1 == 2 and {r[8] for r in rows[1:]} == {"승인 거절"}

    resp = requests.get(_ev(base_url, s, "/issued-tickets/export"), headers=_h(s, "guest"))
    assert_status(resp, 200)
    rows = _xlsx_rows(resp.content)
    assert rows[0][:9] == ["티켓번호", "티켓 종류", "티켓 이름", "주문자", "연락처", "주문번호", "발급일시", "입장", "체크인 시각"]
    assert rows[0][9:] == ["뒷풀이", "입금자명"]
    assert len(rows) - 1 == 5
    rows = _xlsx_rows(requests.get(_ev(base_url, s, "/issued-tickets/export"), params={"entrance": "DONE"}, headers=_h(s, "guest")).content)
    assert len(rows) - 1 == 3
    assert_status(requests.get(_ev(base_url, s, "/orders/export"), headers=_h(s, "outsider")), 403)
