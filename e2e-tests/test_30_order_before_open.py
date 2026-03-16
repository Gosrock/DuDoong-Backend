"""
오픈 전 이벤트 주문 시도 차단 E2E 테스트.
PREPARING 상태 이벤트에 주문을 시도하면 실패하는지 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


_preopen_state: dict = {
    "event_id": 0,
    "ticket_item_id": 0,
    "buyer_headers": {},
}


def _login(base_url: str, email: str, name: str) -> dict:
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


def test_setup_preparing_event(base_url, auth_headers, state):
    """PREPARING 상태(오픈 전)인 이벤트와 티켓을 생성합니다. 오픈하지 않습니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    from datetime import datetime, timedelta
    future = (datetime.now() + timedelta(days=200)).strftime("%Y.%m.%d %H:%M")

    # 이벤트 생성 (PREPARING 상태)
    event_resp = requests.post(
        f"{base_url}/v1/events",
        json={"hostId": state.host_id, "name": "오픈전주문차단테스트", "startAt": future, "runTime": 60},
        headers=auth_headers,
    )
    assert event_resp.status_code in (200, 201), f"이벤트 생성 실패: {event_resp.text[:200]}"
    _preopen_state["event_id"] = get_data(event_resp)["eventId"]

    # 기본 정보 및 상세 설정
    requests.patch(
        f"{base_url}/v1/events/{_preopen_state['event_id']}/basic",
        json={"name": "오픈전주문차단테스트", "startAt": future, "runTime": 60,
              "placeName": "테스트공연장", "placeAddress": "서울시 강남구",
              "longitude": 127.0, "latitude": 37.5},
        headers=auth_headers,
    )
    requests.patch(
        f"{base_url}/v1/events/{_preopen_state['event_id']}/details",
        json={"posterImageKey": "test/event/e2e/poster.jpeg", "content": "오픈 전 주문 차단 테스트"},
        headers=auth_headers,
    )

    # 무료 티켓 생성
    ticket_resp = requests.post(
        f"{base_url}/v1/events/{_preopen_state['event_id']}/ticketItems",
        json={
            "payType": "무료티켓",
            "name": "오픈전테스트티켓",
            "description": "오픈 전 상태의 티켓",
            "price": 0,
            "supplyCount": 50,
            "approveType": "선착순",
            "isQuantityPublic": True,
            "purchaseLimit": 2,
        },
        headers=auth_headers,
    )
    assert ticket_resp.status_code in (200, 201), f"티켓 생성 실패: {ticket_resp.text[:300]}"
    _preopen_state["ticket_item_id"] = get_data(ticket_resp)["ticketItemId"]

    # 구매자 로그인 (이벤트는 오픈하지 않음!)
    buyer_data = _login(base_url, "preopen-buyer@dudoong.com", "오픈전구매자")
    _preopen_state["buyer_headers"] = {"Authorization": f"Bearer {buyer_data['accessToken']}"}

    print(f"[setup] PREPARING 이벤트 셋업 완료: event={_preopen_state['event_id']} (오픈 안 함)")


def test_cart_before_open_fails(base_url):
    """오픈 전 이벤트의 티켓으로 장바구니를 생성하면 실패합니다."""
    ticket_id = _preopen_state.get("ticket_item_id")
    if not ticket_id or not _preopen_state["buyer_headers"]:
        pytest.skip("셋업 안 됨")

    url = f"{base_url}/v1/carts"
    payload = {"items": [{"itemId": ticket_id, "quantity": 1, "options": []}]}
    print(f"\n[test_cart_before_open] POST {url}")
    resp = requests.post(url, json=payload, headers=_preopen_state["buyer_headers"])
    print(f"[test_cart_before_open] status={resp.status_code}, body={resp.text[:400]}")

    # 장바구니 단계에서 차단되거나, 주문 단계에서 차단될 수 있음
    if resp.status_code in (400, 403, 409, 422):
        print(f"[test_cart_before_open] 장바구니 단계에서 오픈 전 차단 확인: {resp.status_code}")
        return

    if resp.status_code not in (200, 201):
        print(f"[test_cart_before_open] 장바구니 실패: {resp.status_code}")
        return

    # 장바구니가 성공하면 주문 단계에서 차단되는지 확인
    cart_id = get_data(resp)["cartId"]
    order_resp = requests.post(
        f"{base_url}/v1/orders/",
        json={"couponId": None, "cartId": cart_id},
        headers=_preopen_state["buyer_headers"],
    )
    print(f"[test_cart_before_open] order status={order_resp.status_code}")

    if order_resp.status_code in (400, 403, 409, 422):
        print(f"[test_cart_before_open] 주문 단계에서 오픈 전 차단 확인: {order_resp.status_code}")
        return

    if order_resp.status_code not in (200, 201):
        return

    # 주문도 성공하면 결제 단계에서라도 차단되어야 함
    order_uuid = get_data(order_resp)["orderId"]
    free_resp = requests.post(
        f"{base_url}/v1/orders/{order_uuid}/free",
        headers=_preopen_state["buyer_headers"],
    )
    print(f"[test_cart_before_open] free status={free_resp.status_code}")
    assert free_resp.status_code != 200, (
        f"오픈 전 이벤트의 주문이 완료되어서는 안 됩니다: {free_resp.status_code}"
    )
    print(f"[test_cart_before_open] 결제 단계에서 오픈 전 차단 확인: {free_resp.status_code}")


def test_event_still_preparing(base_url):
    """이벤트가 여전히 PREPARING 상태인지 확인합니다."""
    event_id = _preopen_state.get("event_id")
    if not event_id:
        pytest.skip("셋업 안 됨")

    url = f"{base_url}/v1/events/{event_id}"
    print(f"\n[test_event_still_preparing] GET {url}")
    resp = requests.get(url)
    print(f"[test_event_still_preparing] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (200, 400)
    if resp.status_code == 400:
        print("[test_event_still_preparing] 비오픈 이벤트 조회 제한 확인 (400)")
        return
    data = get_data(resp)
    status = data.get("status", data.get("eventStatus", ""))
    print(f"[test_event_still_preparing] 이벤트 상태: {status}")
    if status:
        assert "PREPARING" in status.upper(), f"이벤트가 PREPARING이 아닙니다: {status}"
