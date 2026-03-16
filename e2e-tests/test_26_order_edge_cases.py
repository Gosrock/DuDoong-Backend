"""
주문 엣지 케이스 E2E 테스트.
재고 소진, 타 유저 주문 조회 차단, 중복 결제 방지 등을 검증합니다.
"""
import pytest
import requests

from conftest import get_data


# 이 모듈 내에서만 사용하는 로컬 상태
_edge_state: dict = {
    "exhausted_ticket_id": 0,
    "exhausted_event_id": 0,
    "user_a_headers": {},
    "user_a_order_uuid": "",
    "user_b_headers": {},
}


def _login(base_url: str, email: str, name: str) -> dict:
    """테스트 유저 로그인 헬퍼."""
    resp = requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={
            "email": email,
            "name": name,
            "phoneNumber": "010-0000-0000",
            "profileImage": None,
            "marketingAgree": False,
        },
    )
    assert resp.status_code == 200, f"로그인 실패: {resp.text}"
    return get_data(resp)


def test_setup_stock_exhaustion(base_url, auth_headers, state):
    """재고 1개 티켓을 생성하고 소진시킵니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    from datetime import datetime, timedelta
    future = (datetime.now() + timedelta(days=120)).strftime("%Y.%m.%d %H:%M")

    # 새 이벤트 생성
    event_resp = requests.post(
        f"{base_url}/v1/events",
        json={"hostId": state.host_id, "name": "재고소진테스트이벤트", "startAt": future, "runTime": 60},
        headers=auth_headers,
    )
    if event_resp.status_code not in (200, 201):
        pytest.skip(f"이벤트 생성 실패: {event_resp.text[:200]}")
    _edge_state["exhausted_event_id"] = get_data(event_resp)["eventId"]

    # 상세 정보 설정
    requests.patch(
        f"{base_url}/v1/events/{_edge_state['exhausted_event_id']}/detail",
        json={"content": "재고 소진 테스트"},
        headers=auth_headers,
    )

    # 재고 1개 티켓 생성
    ticket_resp = requests.post(
        f"{base_url}/v1/events/{_edge_state['exhausted_event_id']}/ticketItems",
        json={
            "payType": "무료티켓",
            "name": "재고1개티켓",
            "description": "재고가 1개뿐인 티켓",
            "price": 0,
            "supplyCount": 1,
            "approveType": "선착순",
            "isQuantityPublic": True,
            "purchaseLimit": 1,
        },
        headers=auth_headers,
    )
    assert ticket_resp.status_code in (200, 201), f"티켓 생성 실패: {ticket_resp.text[:300]}"
    _edge_state["exhausted_ticket_id"] = get_data(ticket_resp)["ticketItemId"]

    # 이벤트 오픈
    open_resp = requests.patch(
        f"{base_url}/v1/events/{_edge_state['exhausted_event_id']}/open",
        headers=auth_headers,
    )
    assert open_resp.status_code == 200, f"이벤트 오픈 실패: {open_resp.text[:200]}"

    # userA로 재고 소진
    data_a = _login(base_url, "stock-userA@dudoong.com", "재고테스터A")
    _edge_state["user_a_headers"] = {"Authorization": f"Bearer {data_a['accessToken']}"}

    cart_resp = requests.post(
        f"{base_url}/v1/carts",
        json={"items": [{"itemId": _edge_state["exhausted_ticket_id"], "quantity": 1, "options": []}]},
        headers=_edge_state["user_a_headers"],
    )
    assert cart_resp.status_code in (200, 201)
    cart_id = get_data(cart_resp)["cartId"]

    order_resp = requests.post(
        f"{base_url}/v1/orders/",
        json={"couponId": None, "cartId": cart_id},
        headers=_edge_state["user_a_headers"],
    )
    assert order_resp.status_code in (200, 201)
    order_uuid = get_data(order_resp)["orderId"]
    _edge_state["user_a_order_uuid"] = order_uuid

    free_resp = requests.post(
        f"{base_url}/v1/orders/{order_uuid}/free",
        headers=_edge_state["user_a_headers"],
    )
    assert free_resp.status_code == 200, f"무료결제 실패: {free_resp.text[:200]}"
    print(f"[test_setup_stock_exhaustion] 재고 소진 완료: ticket={_edge_state['exhausted_ticket_id']}")


def test_order_after_stock_exhausted(base_url):
    """재고 소진 후 주문을 시도하면 실패합니다."""
    ticket_id = _edge_state.get("exhausted_ticket_id")
    if not ticket_id:
        pytest.skip("재고 소진 셋업이 안 됨")

    data_b = _login(base_url, "stock-userB@dudoong.com", "재고테스터B")
    _edge_state["user_b_headers"] = {"Authorization": f"Bearer {data_b['accessToken']}"}

    # 장바구니 생성 시도
    cart_resp = requests.post(
        f"{base_url}/v1/carts",
        json={"items": [{"itemId": ticket_id, "quantity": 1, "options": []}]},
        headers=_edge_state["user_b_headers"],
    )
    print(f"\n[test_order_after_stock_exhausted] cart status={cart_resp.status_code}")

    if cart_resp.status_code in (400, 409, 422):
        print(f"[test_order_after_stock_exhausted] 장바구니 단계에서 재고 부족 감지: {cart_resp.status_code}")
        return

    if cart_resp.status_code not in (200, 201):
        print(f"[test_order_after_stock_exhausted] 장바구니 실패: {cart_resp.status_code}")
        return

    cart_id = get_data(cart_resp)["cartId"]

    # 주문 생성 시도
    order_resp = requests.post(
        f"{base_url}/v1/orders/",
        json={"couponId": None, "cartId": cart_id},
        headers=_edge_state["user_b_headers"],
    )
    print(f"[test_order_after_stock_exhausted] order status={order_resp.status_code}")

    if order_resp.status_code in (400, 409, 422):
        print(f"[test_order_after_stock_exhausted] 주문 단계에서 재고 부족 감지: {order_resp.status_code}")
        return

    if order_resp.status_code not in (200, 201):
        return

    order_uuid = get_data(order_resp)["orderId"]
    free_resp = requests.post(
        f"{base_url}/v1/orders/{order_uuid}/free",
        headers=_edge_state["user_b_headers"],
    )
    print(f"[test_order_after_stock_exhausted] free status={free_resp.status_code}")
    assert free_resp.status_code != 200, (
        f"재고 소진 후 주문이 성공해서는 안 됩니다: {free_resp.status_code}"
    )
    print(f"[test_order_after_stock_exhausted] 재고 부족 차단 확인: {free_resp.status_code}")


def test_other_user_cannot_view_order(base_url):
    """다른 유저의 주문을 조회하면 차단됩니다."""
    order_uuid = _edge_state.get("user_a_order_uuid")
    if not order_uuid or not _edge_state.get("user_b_headers"):
        pytest.skip("테스트 데이터 없음")

    url = f"{base_url}/v1/orders/{order_uuid}"
    print(f"\n[test_other_user_cannot_view_order] GET {url} (userB가 userA 주문 조회)")
    resp = requests.get(url, headers=_edge_state["user_b_headers"])
    print(f"[test_other_user_cannot_view_order] status={resp.status_code}")

    assert resp.status_code in (400, 401, 403, 404), (
        f"타 유저 주문 조회가 차단되어야 합니다: status={resp.status_code}"
    )
    print(f"[test_other_user_cannot_view_order] 타 유저 주문 조회 차단: {resp.status_code}")


def test_duplicate_free_payment_fails(base_url, auth_headers, state):
    """이미 결제된 주문에 다시 무료 결제를 시도하면 실패합니다."""
    if not state.order_uuid:
        pytest.skip("order_uuid가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/orders/{state.order_uuid}/free"
    print(f"\n[test_duplicate_free_payment_fails] POST {url} (중복 결제 시도)")
    resp = requests.post(url, headers=auth_headers)
    print(f"[test_duplicate_free_payment_fails] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code != 200, (
        f"이미 결제된 주문의 중복 결제가 성공해서는 안 됩니다: {resp.status_code}"
    )
    print(f"[test_duplicate_free_payment_fails] 중복 결제 차단 확인: {resp.status_code}")


def test_unauthenticated_order_attempt(base_url, state):
    """인증 없이 주문을 시도하면 401/403이 반환됩니다."""
    if not state.ticket_item_id:
        pytest.skip("ticket_item_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/carts"
    payload = {"items": [{"itemId": state.ticket_item_id, "quantity": 1, "options": []}]}
    print(f"\n[test_unauthenticated_order_attempt] POST {url} (인증 없음)")
    resp = requests.post(url, json=payload)
    print(f"[test_unauthenticated_order_attempt] status={resp.status_code}")

    assert resp.status_code in (401, 403), (
        f"인증 없는 주문 시도가 차단되어야 합니다: status={resp.status_code}"
    )
    print(f"[test_unauthenticated_order_attempt] 미인증 차단: {resp.status_code}")
