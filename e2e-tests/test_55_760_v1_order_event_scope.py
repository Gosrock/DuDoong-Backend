"""
v1 호스트 주문·환불 API 공연 소속 검증 E2E 테스트 (#760).

시나리오: 호스트 A(매니저)·호스트 B 각각 공연·두둥티켓 준비 → B 공연에 주문(승인 대기·승인·거절=환불 요청) →
A 매니저가 A 공연 경로로 B 주문의 승인·거절·취소·환불 완료·주문 상세·환불 상세를 요청하면 모두 404(Order_404_1)이고
B 쪽에서 본 주문 상태·발급 티켓·환불 상태는 그대로다. 같은 공연 주문은 기존처럼 처리된다.

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
SECTIONS = [{"title": "공연 소개", "content": "<p>공연 소속 검증</p>", "sortOrder": 0}]
ACCOUNT = {"bank": "신한은행", "holder": "고스락", "number": "110-123-456789"}


class ScopeState:
    tokens: dict = {}
    emails: dict = {}
    events: dict = {}
    tickets: dict = {}
    orders: dict = {}


@pytest.fixture(scope="module")
def s():
    return ScopeState()


def _h(s, who):
    return {"Authorization": f"Bearer {s.tokens[who]}"}


def _login(base_url, email, name):
    resp = requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={"email": email, "name": name, "phoneNumber": "010-0000-0000", "profileImage": None, "marketingAgree": False},
    )
    assert_status(resp, 200)
    return get_data(resp)["accessToken"]


def _v1_order(base_url, s, who, side):
    cart = requests.post(
        f"{base_url}/v1/carts", json={"items": [{"itemId": s.tickets[side], "quantity": 1, "options": []}]}, headers=_h(s, who),
    )
    assert cart.status_code in (200, 201), cart.text[:300]
    resp = requests.post(f"{base_url}/v1/orders/", json={"cartId": get_data(cart)["cartId"], "couponId": None}, headers=_h(s, who))
    assert resp.status_code in (200, 201), resp.text[:300]
    return get_data(resp)["orderId"]


def _v1(base_url, s, side):
    return f"{base_url}/v1/events/{s.events[side]}"


def _b_order(base_url, s, order_uuid):
    """B 호스트가 v2 로 본 주문 상세 (상태 확인용)"""
    resp = requests.get(f"{base_url}/v2/events/{s.events['b']}/orders/{order_uuid}", headers=_h(s, "b_master"))
    assert_status(resp, 200)
    return get_data(resp)


def _assert_not_found(resp):
    assert_status(resp, 404)
    assert resp.json().get("code") == "Order_404_1"


def test_01_setup(base_url, s):
    for who, name in [("a_master", "소속A마스터"), ("a_manager", "소속A매니저"), ("b_master", "소속B마스터"), ("buyer", "소속구매자")]:
        email = f"scope760-{who}-{RUN}@dudoong.com"
        s.tokens[who], s.emails[who] = _login(base_url, email, name), email

    for side, who in [("a", "a_master"), ("b", "b_master")]:
        resp = requests.post(
            f"{base_url}/v2/hosts", json={"name": f"소속{side.upper()}{RUN[:5]}", "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]},
            headers=_h(s, who),
        )
        assert_status(resp, 200)
        host_id = get_data(resp)["hostId"]
        if side == "a":
            resp = requests.post(
                f"{base_url}/v2/hosts/{host_id}/members", json={"members": [{"email": s.emails["a_manager"], "role": "MANAGER"}]}, headers=_h(s, who),
            )
            assert_status(resp, 200)
        resp = requests.post(
            f"{base_url}/v2/events",
            json={"hostId": host_id, "name": "소속검증공연", "startAt": START.strftime(FMT), "endAt": END.strftime(FMT), "hasTicket": True},
            headers=_h(s, who),
        )
        assert_status(resp, 200)
        event_id = get_data(resp)["eventId"]
        s.events[side] = event_id
        resp = requests.post(
            f"{base_url}/v2/events/{event_id}/ticket-items",
            json={"payType": "DUDOONG", "name": "일반", "description": "일반 입장", "price": 6000, "supplyCount": 30, "account": ACCOUNT,
                  "approvalRequired": True, "isQuantityPublic": True, "purchaseLimit": 4, "saleStartAt": None, "saleEndAt": None},
            headers=_h(s, who),
        )
        assert_status(resp, 200)
        s.tickets[side] = get_data(resp)["ticketItemId"]
        key = get_data(requests.post(f"{base_url}/v2/events/{event_id}/images", json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, who)))["key"]
        resp = requests.patch(
            f"{base_url}/v2/events/{event_id}/basic",
            json={"posterImageKey": key, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]},
            headers=_h(s, who),
        )
        assert_status(resp, 200)
        assert_status(requests.put(f"{base_url}/v2/events/{event_id}/sections", json=SECTIONS, headers=_h(s, who)), 200)
        assert_status(requests.post(f"{base_url}/v2/events/{event_id}/open", headers=_h(s, who)), 200)

    # B 공연 주문: 승인 대기 / 승인 완료 / 거절(환불 요청), A 공연 주문: 승인 대기
    for key in ("pending", "approved", "refunding"):
        s.orders[key] = _v1_order(base_url, s, "buyer", "b")
    s.orders["mine"] = _v1_order(base_url, s, "buyer", "a")
    assert_status(requests.post(f"{_v1(base_url, s, 'b')}/orders/{s.orders['approved']}/approve", headers=_h(s, "b_master")), 200)
    resp = requests.post(
        f"{base_url}/v2/events/{s.events['b']}/orders/{s.orders['refunding']}/refuse", json={"reasonType": "SOLD_OUT"}, headers=_h(s, "b_master"),
    )
    assert_status(resp, 200)
    assert get_data(resp)["order"]["refundStatus"] == "REQUESTED"


def test_02_approve_other_event_order_404(base_url, s):
    _assert_not_found(requests.post(f"{_v1(base_url, s, 'a')}/orders/{s.orders['pending']}/approve", headers=_h(s, "a_manager")))
    data = _b_order(base_url, s, s.orders["pending"])
    assert data["order"]["status"] == "PENDING_APPROVE"
    assert data["issuedTickets"] == []
    # 같은 공연 주문은 기존대로 승인
    assert_status(requests.post(f"{_v1(base_url, s, 'a')}/orders/{s.orders['mine']}/approve", headers=_h(s, "a_manager")), 200)


def test_03_refuse_other_event_order_404(base_url, s):
    resp = requests.post(f"{_v1(base_url, s, 'a')}/orders/{s.orders['pending']}/refuse", json={"reason": "사유"}, headers=_h(s, "a_manager"))
    _assert_not_found(resp)
    order = _b_order(base_url, s, s.orders["pending"])["order"]
    assert order["status"] == "PENDING_APPROVE" and order["refundStatus"] == "NONE"


def test_04_cancel_other_event_order_404(base_url, s):
    before = _b_order(base_url, s, s.orders["approved"])
    assert before["order"]["status"] == "APPROVED" and before["issuedTickets"]
    resp = requests.post(f"{_v1(base_url, s, 'a')}/orders/{s.orders['approved']}/cancel", json={"reason": "사유"}, headers=_h(s, "a_manager"))
    _assert_not_found(resp)
    after = _b_order(base_url, s, s.orders["approved"])
    assert after["order"]["status"] == "APPROVED"
    assert [t["entrance"] for t in after["issuedTickets"]] == [t["entrance"] for t in before["issuedTickets"]]
    assert "CANCELED" not in {t["entrance"] for t in after["issuedTickets"]}


def test_05_refund_complete_other_event_order_404(base_url, s):
    _assert_not_found(requests.patch(f"{_v1(base_url, s, 'a')}/refunds/{s.orders['refunding']}/complete", headers=_h(s, "a_manager")))
    assert _b_order(base_url, s, s.orders["refunding"])["order"]["refundStatus"] == "REQUESTED"


def test_06_details_other_event_order_404(base_url, s):
    for key in ("pending", "approved", "refunding"):
        _assert_not_found(requests.get(f"{_v1(base_url, s, 'a')}/orders/{s.orders[key]}", headers=_h(s, "a_manager")))
    _assert_not_found(requests.get(f"{_v1(base_url, s, 'a')}/refunds/{s.orders['refunding']}", headers=_h(s, "a_manager")))
    # 같은 공연 경로로는 기존대로 조회
    assert_status(requests.get(f"{_v1(base_url, s, 'b')}/orders/{s.orders['pending']}", headers=_h(s, "b_master")), 200)
    assert_status(requests.get(f"{_v1(base_url, s, 'b')}/refunds/{s.orders['refunding']}", headers=_h(s, "b_master")), 200)


def test_07_same_event_refund_complete_still_works(base_url, s):
    assert_status(requests.patch(f"{_v1(base_url, s, 'b')}/refunds/{s.orders['refunding']}/complete", headers=_h(s, "b_master")), 200)
    assert _b_order(base_url, s, s.orders["refunding"])["order"]["refundStatus"] == "COMPLETED"
