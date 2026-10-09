"""
v2 API 컨벤션 통일 E2E 테스트 (#755).

시나리오: T-3 부분 수정(null = 변경 안 함, '값 없음'은 clear, 판매된 티켓 잠금 규칙 유지) → 바뀐 이름·경로
(계좌 bankName/accountHolder/accountNumber, 주문 생성 POST /me/orders · paymentChannel, 호스팅 주문 quantity/totalAmount,
D-1 entranceStats, 체크리스트 isBasicFilled/isDetailFilled/hasValidTicket, E-6 {sections}, 알림 notificationId/targetId,
G-7 isGiftExpired, transfer-master, refundAccount.updatedAt 형식) → Swagger(사용자앱 그룹 노출, 공통 래퍼, 날짜 pattern, 역할 enum).

재실행해도 충돌하지 않도록 유저 이메일에 실행마다 다른 접미사를 붙인다.
"""
import base64
import json
import re
import time
import uuid
from datetime import datetime, timedelta
from urllib.parse import quote

import pytest
import requests

from conftest import assert_status, get_data

RUN = uuid.uuid4().hex[:8]
FMT = "%Y.%m.%d %H:%M"
DATE_RE = re.compile(r"\d{4}\.\d{2}\.\d{2} \d{2}:\d{2}")
START = (datetime.now() + timedelta(days=30)).replace(hour=18, minute=0, second=0, microsecond=0)
END = START + timedelta(minutes=120)
PLACE = {"name": "롤링홀", "address": "서울 마포구 어울마당로 35", "latitude": 37.548369, "longitude": 126.920036}
SECTIONS = [{"title": "공연 소개", "content": "<p>컨벤션</p>", "sortOrder": 0}]
ACCOUNT = {"bankName": "신한은행", "accountHolder": "고스락", "accountNumber": "110-123-456789"}
PEOPLE = ["master", "manager", "guest", "buyer", "sender", "receiver"]


class State:
    tokens: dict = {}
    emails: dict = {}
    user_ids: dict = {}
    host_id: int = 0
    event_id: int = 0
    ticket_id: int = 0


@pytest.fixture(scope="module")
def s():
    return State()


def _h(s, who):
    return {"Authorization": f"Bearer {s.tokens[who]}"}


def _code(resp):
    return resp.json().get("code")


def _ev(base_url, s, path=""):
    return f"{base_url}/v2/events/{s.event_id}{path}"


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


def _ticket(base_url, s, ticket_id):
    items = get_data(requests.get(_ev(base_url, s, "/ticket-items/manage"), headers=_h(s, "guest")))
    return next(t for t in items if t["ticketItemId"] == ticket_id)


def _patch(base_url, s, ticket_id, body):
    return requests.patch(_ev(base_url, s, f"/ticket-items/{ticket_id}"), json=body, headers=_h(s, "manager"))


def _docs(base_url, s, group):
    root = base_url[: -len("/api")] if base_url.endswith("/api") else base_url
    resp = requests.get(f"{root}/v3/api-docs/{quote(group)}", headers=_h(s, "buyer"))
    assert_status(resp, 200)
    return resp.json()


def test_01_setup(base_url, s):
    for who in PEOPLE:
        email = f"conv755-{who}-{RUN}@dudoong.com"
        s.tokens[who], s.user_ids[who] = _login(base_url, email, f"컨벤{who}")
        s.emails[who] = email
    resp = requests.post(f"{base_url}/v2/hosts", json={"name": f"컨벤{RUN[:5]}", "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    s.host_id = get_data(resp)["hostId"]
    resp = requests.post(
        f"{base_url}/v2/hosts/{s.host_id}/members",
        json={"members": [{"email": s.emails["manager"], "role": "MANAGER"}, {"email": s.emails["guest"], "role": "GUEST"}]},
        headers=_h(s, "master"),
    )
    assert_status(resp, 200)
    resp = requests.post(
        f"{base_url}/v2/events",
        json={"hostId": s.host_id, "name": "컨벤션공연", "startAt": START.strftime(FMT), "endAt": END.strftime(FMT), "hasTicket": True},
        headers=_h(s, "manager"),
    )
    assert_status(resp, 200)
    s.event_id = get_data(resp)["eventId"]
    # 체크리스트 Boolean 이름 (#755 C19)
    checklist = get_data(requests.get(_ev(base_url, s, "/checklist"), headers=_h(s, "guest")))
    assert checklist == {"isBasicFilled": False, "isDetailFilled": False, "hasValidTicket": False, "ticketRequired": True, "canOpen": False}


def test_02_ticket_create_account_names(base_url, s):
    resp = requests.post(_ev(base_url, s, "/ticket-items"), json={
        "payType": "DUDOONG", "name": "일반", "description": "일반 입장", "price": 5000, "supplyCount": 100, "account": ACCOUNT,
        "approvalRequired": True, "isQuantityPublic": True, "purchaseLimit": 4, "saleStartAt": None, "saleEndAt": (START - timedelta(days=1)).strftime(FMT),
    }, headers=_h(s, "manager"))
    assert_status(resp, 200)
    data = get_data(resp)
    s.ticket_id = data["ticketItemId"]
    assert data["account"] == ACCOUNT


def test_03_ticket_patch_partial_and_clear(base_url, s):
    sale_end = (START - timedelta(days=1)).strftime(FMT)
    # 바꾼 필드만 — 나머지는 그대로 (예전에는 빠진 선택 필드가 무제한·없음으로 바뀌었다)
    data = get_data(_patch(base_url, s, s.ticket_id, {"name": "일반석"}))
    assert data["name"] == "일반석" and data["supplyCount"] == 100 and data["purchaseLimit"] == 4
    assert data["saleEndAt"] == sale_end and data["description"] == "일반 입장" and data["account"] == ACCOUNT and data["price"] == 5000
    # 명시적 null 도 변경 안 함
    data = get_data(_patch(base_url, s, s.ticket_id, {"supplyCount": None, "purchaseLimit": None, "saleEndAt": None}))
    assert data["supplyCount"] == 100 and data["purchaseLimit"] == 4 and data["saleEndAt"] == sale_end
    # 지금처럼 전체 값을 보내도 같은 결과
    full = {"payType": "DUDOONG", "name": "일반석", "description": "일반 입장", "price": 5000, "supplyCount": 100, "account": ACCOUNT,
            "approvalRequired": True, "isQuantityPublic": True, "purchaseLimit": 4, "saleStartAt": None, "saleEndAt": sale_end}
    assert get_data(_patch(base_url, s, s.ticket_id, full)) == data
    # '값 없음'은 clear
    data = get_data(_patch(base_url, s, s.ticket_id, {"clear": ["PURCHASE_LIMIT", "SALE_END_AT", "DESCRIPTION"]}))
    assert data["purchaseLimit"] is None and data["saleEndAt"] is None and data["description"] is None and data["supplyCount"] == 100
    resp = _patch(base_url, s, s.ticket_id, {"purchaseLimit": 2, "clear": ["PURCHASE_LIMIT"]})
    assert resp.status_code == 400 and _code(resp) == "Ticket_Item_400_15"
    resp = _patch(base_url, s, s.ticket_id, {"clear": ["SUPPLY_COUNT"]})  # 재고 공개 중이면 무제한 불가
    assert resp.status_code == 400 and _code(resp) == "Ticket_Item_400_15"
    assert_status(_patch(base_url, s, s.ticket_id, {"purchaseLimit": 4, "description": "일반 입장"}), 200)


def test_04_open_with_sections_object(base_url, s):
    key = get_data(requests.post(_ev(base_url, s, "/images"), json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "manager")))["key"]
    assert_status(requests.patch(_ev(base_url, s, "/basic"), json={"posterImageKey": key, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]}, headers=_h(s, "manager")), 200)
    # E-6 본문은 객체 {sections} (#755 C29). 예전 맨 배열은 400
    assert requests.put(_ev(base_url, s, "/sections"), json=SECTIONS, headers=_h(s, "manager")).status_code == 400
    resp = requests.put(_ev(base_url, s, "/sections"), json={"sections": SECTIONS}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert [x["title"] for x in get_data(resp)] == ["공연 소개"]
    checklist = get_data(requests.get(_ev(base_url, s, "/checklist"), headers=_h(s, "guest")))
    assert checklist["isBasicFilled"] and checklist["isDetailFilled"] and checklist["hasValidTicket"] and checklist["canOpen"]
    assert_status(requests.post(_ev(base_url, s, "/open"), headers=_h(s, "manager")), 200)


def test_05_order_path_and_names(base_url, s):
    body = {"eventId": s.event_id, "ticketItemId": s.ticket_id, "quantity": 2, "options": {"applyToAll": True, "answers": []},
            "perTicketOptions": None, "paymentChannel": "BANK_TRANSFER", "depositorName": "컨벤입금", "agreeRefundPolicy": True}
    # 옛 경로는 없다
    assert requests.post(f"{base_url}/v2/orders", json=body, headers=_h(s, "buyer")).status_code in (404, 405)
    resp = requests.post(f"{base_url}/v2/me/orders", json=body, headers=_h(s, "buyer"))
    assert_status(resp, 200)
    order = get_data(resp)
    assert order["paymentChannel"] == "BANK_TRANSFER" and order["status"] == "PENDING_APPROVE"
    assert order["payment"]["account"] == ACCOUNT and order["canEditRefundAccount"] is False
    s.order = order["orderUuid"]
    listed = get_data(requests.get(_ev(base_url, s, "/orders"), headers=_h(s, "guest")))["orders"]["content"]
    row = next(o for o in listed if o["orderUuid"] == s.order)
    assert row["quantity"] == 2 and row["totalAmount"] == 10000 and "totalQuantity" not in row and "totalPaymentAmount" not in row
    assert_status(requests.post(_ev(base_url, s, f"/orders/{s.order}/approve"), headers=_h(s, "manager")), 200)
    dashboard = get_data(requests.get(_ev(base_url, s, "/dashboard"), headers=_h(s, "guest")))
    assert dashboard["entranceStats"]["issuedCount"] == 2 and "entrance" not in dashboard


def test_06_refund_account_updated_at_format(base_url, s):
    resp = requests.post(f"{base_url}/v2/me/orders/{s.order}/cancel", json={"refundAccount": {"bankName": "국민은행", "accountHolder": "구매자", "accountNumber": "123456789012"}}, headers=_h(s, "buyer"))
    assert_status(resp, 200)
    detail = get_data(requests.get(_ev(base_url, s, f"/orders/{s.order}"), headers=_h(s, "manager")))
    assert DATE_RE.fullmatch(detail["refundAccount"]["updatedAt"]), detail["refundAccount"]
    refunds = get_data(requests.get(_ev(base_url, s, "/refunds"), params={"status": "ALL"}, headers=_h(s, "guest")))
    assert any(r["orderUuid"] == s.order and r["totalAmount"] == 10000 for r in refunds["content"])


def test_07_notification_and_gift_names(base_url, s):
    deadline = time.time() + 10
    content = []
    while time.time() < deadline:
        content = get_data(requests.get(f"{base_url}/v2/me/notifications", headers=_h(s, "buyer")))["content"]
        if content:
            break
        time.sleep(0.2)
    assert content and "notificationId" in content[0] and "id" not in content[0]
    assert "targetId" in content[0]["target"] and "id" not in content[0]["target"]
    resp = requests.post(f"{base_url}/v2/me/notifications/read", json={"notificationIds": [content[0]["notificationId"]]}, headers=_h(s, "buyer"))
    assert_status(resp, 200)
    # 무료 즉시 발급 티켓을 하나 더 만들어 선물 → G-7 isGiftExpired
    resp = requests.post(_ev(base_url, s, "/ticket-items"), json={
        "payType": "FREE", "name": "무료", "description": None, "price": 0, "supplyCount": 10, "account": None,
        "approvalRequired": False, "isQuantityPublic": True, "purchaseLimit": None, "saleStartAt": None, "saleEndAt": None,
    }, headers=_h(s, "manager"))
    assert_status(resp, 200)
    free_id = get_data(resp)["ticketItemId"]
    resp = requests.post(f"{base_url}/v2/me/orders", json={"eventId": s.event_id, "ticketItemId": free_id, "quantity": 1, "options": {"applyToAll": True, "answers": []},
                                                         "perTicketOptions": None, "paymentChannel": "FREE", "depositorName": None, "agreeRefundPolicy": True}, headers=_h(s, "sender"))
    assert_status(resp, 200)
    ticket_uuid = get_data(resp)["issuedTickets"][0]["ticketUuid"]
    assert_status(requests.post(f"{base_url}/v2/me/tickets/{ticket_uuid}/gift", json={"memo": None}, headers=_h(s, "sender")), 200)
    gift = get_data(requests.get(f"{base_url}/v2/me/gifts", params={"direction": "SENT"}, headers=_h(s, "sender")))["content"][0]
    assert gift["isGiftExpired"] is False and "isExpired" not in gift
    detail = get_data(requests.get(f"{base_url}/v2/me/tickets/{ticket_uuid}", headers=_h(s, "sender")))
    assert detail["unitPrice"] == 0 and detail["optionAmount"] == 0 and "ticketPrice" not in detail


def test_08_transfer_master_path(base_url, s):
    assert requests.post(f"{base_url}/v2/hosts/{s.host_id}/master-transfer", json={"userId": s.user_ids["manager"]}, headers=_h(s, "master")).status_code in (404, 405)
    resp = requests.post(f"{base_url}/v2/hosts/{s.host_id}/transfer-master", json={"userId": s.user_ids["manager"]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    roles = {m["userId"]: m["role"] for m in get_data(resp)}
    assert roles[s.user_ids["manager"]] == "MASTER" and roles[s.user_ids["master"]] == "MANAGER"


def test_09_swagger(base_url, s):
    user = _docs(base_url, s, "v2-사용자앱")
    for path, method in [("/api/v2/me/notifications", "get"), ("/api/v2/check-ins/self", "post"), ("/api/v2/hosts/{hostId}", "get"),
                         ("/api/v2/hosts/{hostId}/follow", "put"), ("/api/v2/hosts/{hostId}/events", "get"), ("/api/v2/events/{eventId}/sections", "get"), ("/api/v2/tags", "get")]:
        assert method in user["paths"].get(path, {}), path
    assert "/api/v2/me/orders" in user["paths"] and "post" in user["paths"]["/api/v2/me/orders"]
    all_docs = _docs(base_url, s, "v2-전체")
    schemas = all_docs["components"]["schemas"]
    ok = all_docs["paths"]["/api/v2/me"]["get"]["responses"]["200"]["content"]["application/json"]["schema"]
    assert set(ok["properties"]) == {"success", "status", "data", "timeStamp"}
    assert not [f"{n}.{f}" for n, sc in schemas.items() for f, p in (sc.get("properties") or {}).items() if p.get("format") == "date-time"]
    assert schemas["V2UpdateHostMemberRoleRequest"]["properties"]["role"]["enum"] == ["MASTER", "MANAGER", "GUEST"]
    assert set(schemas["V2TicketAccountResponse"]["properties"]) == {"bankName", "accountHolder", "accountNumber"}
    assert "clear" in schemas["V2UpdateTicketItemRequest"]["properties"]
    assert "paymentChannel" in schemas["V2CreateOrderRequest"]["properties"] and "paymentMethod" not in schemas["V2CreateOrderRequest"]["properties"]
    excel = all_docs["paths"]["/api/v2/events/{eventId}/orders/export"]["get"]["responses"]["200"]["content"]
    assert list(excel) == ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"]
