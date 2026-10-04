"""
#720 회귀: 승인형 주문 생성 재고 검사는 같은 티켓의 승인 대기 수량만 더한다 (MySQL).

예전에는 공연 전체 승인 대기 수량을 이 티켓 재고와 비교해, 승인형 티켓 2종 이상 공연에서
다른 티켓 대기가 쌓이면 재고가 남은 티켓도 Ticket_Item_400_1(재고 부족)로 거절했다.

시나리오: 두둥 승인형 티켓 A(재고 3)·B(재고 20) 공연 → B 승인 대기 4장 (> A 재고) →
(a) A 주문 v1 2장 / v2 1장 성공 (A 대기 3 = 재고 3, 경계) → (b) A 1장 더는 v1·v2 모두 거절, B 는 계속 주문 가능 →
승인(v1 2장, v2 1장: MySQL 에서는 남은 재고 절반 초과 승인도 성공) 후 재고 0, 발급 3장, 더 주문하면 거절 (초과 판매 없음).

재실행해도 충돌하지 않도록 유저 이메일에 실행마다 다른 접미사를 붙인다. DB 직접 접근은 하지 않는다.
"""
import uuid
from datetime import datetime, timedelta

import pytest
import requests

from conftest import assert_status, get_data

RUN = uuid.uuid4().hex[:8]
FMT = "%Y.%m.%d %H:%M"
START = (datetime.now() + timedelta(days=30)).replace(hour=18, minute=0, second=0, microsecond=0)
END = START + timedelta(minutes=120)
PLACE = {"name": "롤링홀", "address": "서울 마포구 어울마당로 35", "latitude": 37.548369, "longitude": 126.920036}
ACCOUNT = {"bank": "신한은행", "holder": "고스락", "number": "110-123-456789"}
BUYERS = ["b1", "b2", "a_v1", "a_v2", "late", "b_more"]
QUANTITY_LACK = "Ticket_Item_400_1"


class StockState:
    tokens: dict = {}
    host_id: int = 0
    event_id: int = 0
    a: int = 0
    b: int = 0
    orders: dict = {}


@pytest.fixture(scope="module")
def s():
    return StockState()


def _h(s, who):
    return {"Authorization": f"Bearer {s.tokens[who]}"}


def _ev(base_url, s, path):
    return f"{base_url}/v2/events/{s.event_id}{path}"


def _login(base_url, email, name):
    resp = requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={"email": email, "name": name, "phoneNumber": "010-0000-0000", "profileImage": None, "marketingAgree": False},
    )
    assert_status(resp, 200)
    return get_data(resp)["accessToken"]


def _ticket_body(name, supply):
    return {"payType": "DUDOONG", "name": name, "description": "승인형", "price": 5000, "supplyCount": supply, "account": ACCOUNT,
            "approvalRequired": True, "isQuantityPublic": True, "purchaseLimit": 4, "saleStartAt": None, "saleEndAt": None}


def _v1_order(base_url, s, who, item_id, quantity):
    cart = requests.post(f"{base_url}/v1/carts", json={"items": [{"itemId": item_id, "quantity": quantity, "options": []}]}, headers=_h(s, who))
    if cart.status_code != 200:
        return cart  # 재고 0 이면 v1 장바구니 단계에서 재고 부족 (CartValidator)
    return requests.post(f"{base_url}/v1/orders/", json={"cartId": get_data(cart)["cartId"], "couponId": None}, headers=_h(s, who))


def _v2_order(base_url, s, who, item_id, quantity):
    body = {
        "eventId": s.event_id, "ticketItemId": item_id, "quantity": quantity,
        "options": {"applyToAll": True, "answers": []}, "perTicketOptions": None,
        "paymentMethod": "BANK_TRANSFER", "depositorName": "입금자", "agreeRefundPolicy": True,
    }
    return requests.post(f"{base_url}/v2/orders", json=body, headers=_h(s, who))


def _lack(resp):
    assert_status(resp, 400)
    assert resp.json()["code"] == QUANTITY_LACK, resp.text


def _manage(base_url, s, item_id):
    resp = requests.get(_ev(base_url, s, "/ticket-items/manage"), headers=_h(s, "manager"))
    assert_status(resp, 200)
    return next(t for t in get_data(resp) if t["ticketItemId"] == item_id)


def test_01_setup(base_url, s):
    for who in ["master", "manager"] + BUYERS:
        s.tokens[who] = _login(base_url, f"fix720-{who}-{RUN}@dudoong.com", f"재고{who}")
    resp = requests.post(f"{base_url}/v2/hosts", json={"name": f"재고{RUN[:5]}", "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    s.host_id = get_data(resp)["hostId"]
    resp = requests.post(f"{base_url}/v2/hosts/{s.host_id}/members", json={"members": [{"email": f"fix720-manager-{RUN}@dudoong.com", "role": "MANAGER"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    resp = requests.post(
        f"{base_url}/v2/events",
        json={"hostId": s.host_id, "name": "720재고공연", "startAt": START.strftime(FMT), "endAt": END.strftime(FMT), "hasTicket": True},
        headers=_h(s, "manager"),
    )
    assert_status(resp, 200)
    s.event_id = get_data(resp)["eventId"]
    for attr, name, supply in [("a", "A석", 3), ("b", "B석", 20)]:
        resp = requests.post(_ev(base_url, s, "/ticket-items"), json=_ticket_body(name, supply), headers=_h(s, "manager"))
        assert_status(resp, 200)
        setattr(s, attr, get_data(resp)["ticketItemId"])

    key = get_data(requests.post(_ev(base_url, s, "/images"), json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "manager")))["key"]
    resp = requests.patch(_ev(base_url, s, "/basic"), json={"posterImageKey": key, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert_status(requests.put(_ev(base_url, s, "/sections"), json=[{"title": "소개", "content": "<p>720</p>", "sortOrder": 0}], headers=_h(s, "manager")), 200)
    assert_status(requests.post(_ev(base_url, s, "/open"), headers=_h(s, "manager")), 200)


def test_02_other_ticket_pending_does_not_block(base_url, s):
    # B 승인 대기 4장 (> A 재고 3)
    s.orders["b1"] = get_data(_v1_order(base_url, s, "b1", s.b, 2))["orderId"]
    s.orders["b2"] = get_data(_v2_order(base_url, s, "b2", s.b, 2))["orderUuid"]
    assert _manage(base_url, s, s.b)["hasPendingOrders"] is True

    # (a) A 주문: v1 2장 성공 (수정 전: 공연 대기 4 + 2 > 3 으로 거절)
    resp = _v1_order(base_url, s, "a_v1", s.a, 2)
    assert_status(resp, 200)
    s.orders["a_v1"] = get_data(resp)["orderId"]
    # (c) v2 1장: A 대기 2 + 1 = 재고 3 (경계) 성공
    resp = _v2_order(base_url, s, "a_v2", s.a, 1)
    assert_status(resp, 200)
    assert get_data(resp)["status"] == "PENDING_APPROVE"
    s.orders["a_v2"] = get_data(resp)["orderUuid"]


def test_03_same_ticket_pending_over_stock_rejected(base_url, s):
    # (b) A 대기 3 + 1 > 재고 3 → v1·v2 모두 재고 부족
    _lack(_v1_order(base_url, s, "late", s.a, 1))
    _lack(_v2_order(base_url, s, "late", s.a, 1))
    # B 는 영향 없음
    assert_status(_v2_order(base_url, s, "b_more", s.b, 1), 200)
    a = _manage(base_url, s, s.a)
    assert a["remaining"] == 3 and a["soldCount"] == 0


def test_04_approve_rechecks_no_oversell(base_url, s):
    # v1 승인 2장 (남은 재고 3의 절반 초과 — MySQL 에서는 성공)
    assert_status(requests.post(f"{base_url}/v1/events/{s.event_id}/orders/{s.orders['a_v1']}/approve", headers=_h(s, "master")), 200)
    assert _manage(base_url, s, s.a)["remaining"] == 1
    # 재고 1, A 대기 1 → 1장 더는 거절
    _lack(_v2_order(base_url, s, "late", s.a, 1))
    # v2 승인 1장 → 재고 0
    assert_status(requests.post(_ev(base_url, s, f"/orders/{s.orders['a_v2']}/approve"), headers=_h(s, "manager")), 200)
    a = _manage(base_url, s, s.a)
    assert a["remaining"] == 0 and a["soldCount"] == 3
    _lack(_v1_order(base_url, s, "late", s.a, 1))
    # B 승인 대기 주문은 그대로 승인 가능 (A 매진과 무관)
    assert_status(requests.post(f"{base_url}/v1/events/{s.event_id}/orders/{s.orders['b1']}/approve", headers=_h(s, "master")), 200)
    assert _manage(base_url, s, s.b)["remaining"] == 18
