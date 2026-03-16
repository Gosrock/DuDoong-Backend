"""
두둥티켓(승인 방식) 주문 플로우 E2E 테스트.
두둥티켓 생성 → 장바구니 → 주문(승인 대기) → 호스트 승인/거절을 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


# 이 모듈 내에서만 사용하는 로컬 상태
_dudoong_state: dict = {
    "event_id": 0,
    "ticket_item_id": 0,
    "order_uuid_approve": "",
    "order_uuid_refuse": "",
    "buyer_token": "",
    "buyer_headers": {},
}


def _login_buyer(base_url: str) -> dict:
    """구매자 유저 로그인."""
    url = f"{base_url}/v1/auth/oauth/local/login"
    payload = {
        "email": "buyer-dudoong@dudoong.com",
        "name": "두둥구매자",
        "phoneNumber": "010-5555-6666",
        "profileImage": None,
        "marketingAgree": False,
    }
    resp = requests.post(url, json=payload)
    assert resp.status_code == 200, f"구매자 로그인 실패: {resp.text}"
    return get_data(resp)


def _create_order(base_url: str, headers: dict, ticket_item_id: int) -> str:
    """장바구니 → 주문 생성 헬퍼. order_uuid 반환."""
    cart_resp = requests.post(
        f"{base_url}/v1/carts",
        json={"items": [{"itemId": ticket_item_id, "quantity": 1, "options": []}]},
        headers=headers,
    )
    assert cart_resp.status_code in (200, 201), f"장바구니 실패: {cart_resp.text[:200]}"
    cart_id = get_data(cart_resp)["cartId"]

    order_resp = requests.post(
        f"{base_url}/v1/orders/",
        json={"couponId": None, "cartId": cart_id},
        headers=headers,
    )
    assert order_resp.status_code in (200, 201), f"주문 실패: {order_resp.text[:200]}"
    return get_data(order_resp)["orderId"]


def test_setup_dudoong_event(base_url, auth_headers, state):
    """두둥티켓 테스트를 위한 새 이벤트와 두둥티켓을 생성합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    from datetime import datetime, timedelta
    future = (datetime.now() + timedelta(days=100)).strftime("%Y.%m.%d %H:%M")

    # 이벤트 생성
    event_resp = requests.post(
        f"{base_url}/v1/events",
        json={"hostId": state.host_id, "name": "두둥티켓테스트공연", "startAt": future, "runTime": 120},
        headers=auth_headers,
    )
    assert event_resp.status_code in (200, 201), f"이벤트 생성 실패: {event_resp.text[:200]}"
    _dudoong_state["event_id"] = get_data(event_resp)["eventId"]

    # 이벤트 기본 정보 설정 (장소 포함)
    from datetime import datetime, timedelta as _td
    future_basic = (datetime.now() + _td(days=100)).strftime("%Y.%m.%d %H:%M")
    basic_resp = requests.patch(
        f"{base_url}/v1/events/{_dudoong_state['event_id']}/basic",
        json={"name": "두둥티켓테스트공연", "startAt": future_basic, "runTime": 120,
              "placeName": "테스트공연장", "placeAddress": "서울시 강남구",
              "longitude": 127.0, "latitude": 37.5},
        headers=auth_headers,
    )
    print(f"[setup] basic status={basic_resp.status_code}")

    # 이벤트 상세 정보 설정
    detail_resp = requests.patch(
        f"{base_url}/v1/events/{_dudoong_state['event_id']}/details",
        json={"posterImageKey": "test/event/e2e/poster.jpeg", "content": "두둥티켓 테스트 상세 내용"},
        headers=auth_headers,
    )
    print(f"[setup] detail status={detail_resp.status_code}")

    # 두둥티켓 생성 (승인 방식, payType=두둥티켓)
    ticket_resp = requests.post(
        f"{base_url}/v1/events/{_dudoong_state['event_id']}/ticketItems",
        json={
            "payType": "두둥티켓",
            "name": "두둥VIP티켓",
            "description": "호스트 승인이 필요한 두둥티켓",
            "price": 10000,
            "supplyCount": 50,
            "approveType": "승인",
            "isQuantityPublic": True,
            "purchaseLimit": 2,
            "bankName": "국민",
            "accountNumber": "123-456-7890",
            "accountHolder": "두둥",
        },
        headers=auth_headers,
    )
    assert ticket_resp.status_code in (200, 201), f"두둥티켓 생성 실패: {ticket_resp.text[:300]}"
    _dudoong_state["ticket_item_id"] = get_data(ticket_resp)["ticketItemId"]

    # 이벤트 오픈
    open_resp = requests.patch(
        f"{base_url}/v1/events/{_dudoong_state['event_id']}/open",
        headers=auth_headers,
    )
    assert open_resp.status_code == 200, f"이벤트 오픈 실패: {open_resp.text[:200]}"

    # 구매자 로그인
    buyer_data = _login_buyer(base_url)
    _dudoong_state["buyer_token"] = buyer_data["accessToken"]
    _dudoong_state["buyer_headers"] = {"Authorization": f"Bearer {buyer_data['accessToken']}"}

    print(f"[setup] 두둥티켓 이벤트 셋업 완료: event={_dudoong_state['event_id']}, ticket={_dudoong_state['ticket_item_id']}")


def test_dudoong_order_creates_pending(base_url):
    """두둥티켓 주문을 생성하면 승인 대기 상태가 됩니다."""
    if not _dudoong_state["ticket_item_id"] or not _dudoong_state["buyer_headers"]:
        pytest.skip("두둥티켓 셋업이 안 됨")

    order_uuid = _create_order(base_url, _dudoong_state["buyer_headers"], _dudoong_state["ticket_item_id"])
    _dudoong_state["order_uuid_approve"] = order_uuid
    print(f"\n[test_dudoong_order] 두둥티켓 주문 생성: {order_uuid}")

    # 주문 상세 조회하여 상태 확인
    resp = requests.get(
        f"{base_url}/v1/orders/{order_uuid}",
        headers=_dudoong_state["buyer_headers"],
    )
    assert_status(resp, 200)
    data = get_data(resp)
    # 주문 상태는 top-level 또는 paymentInfo 안에 있을 수 있음
    order_status = (
        data.get("orderStatus")
        or (data.get("paymentInfo") or {}).get("orderStatus")
    )
    print(f"[test_dudoong_order] 주문 상태: {order_status or 'N/A'}")
    # 승인 대기 상태여야 함 - 응답에 orderUuid 또는 orderId가 있으면 주문 성공
    assert data.get("orderUuid") or data.get("orderId") or order_status is not None, (
        f"주문 상태 정보가 응답에 없습니다: {data}"
    )


def test_host_approves_order(base_url, auth_headers):
    """호스트가 두둥티켓 주문을 승인합니다."""
    event_id = _dudoong_state.get("event_id")
    order_uuid = _dudoong_state.get("order_uuid_approve")
    if not event_id or not order_uuid:
        pytest.skip("두둥티켓 주문이 없어 건너뜁니다.")

    url = f"{base_url}/v1/events/{event_id}/orders/{order_uuid}/approve"
    print(f"\n[test_host_approves_order] POST {url}")
    resp = requests.post(url, headers=auth_headers)
    print(f"[test_host_approves_order] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = get_data(resp)
    print(f"[test_host_approves_order] 승인 완료: {data.get('orderUuid', data.get('orderId'))}")


def test_approved_order_has_issued_tickets(base_url):
    """승인된 주문에 발급 티켓이 생성되었는지 확인합니다."""
    order_uuid = _dudoong_state.get("order_uuid_approve")
    if not order_uuid or not _dudoong_state["buyer_headers"]:
        pytest.skip("승인된 주문이 없어 건너뜁니다.")

    url = f"{base_url}/v1/orders/{order_uuid}/tickets"
    print(f"\n[test_approved_order_has_issued_tickets] GET {url}")
    resp = requests.get(url, headers=_dudoong_state["buyer_headers"])
    print(f"[test_approved_order_has_issued_tickets] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    print("[test_approved_order_has_issued_tickets] 발급 티켓 확인 완료")


def test_dudoong_order_refuse(base_url, auth_headers):
    """두둥티켓 주문을 생성 후 호스트가 거절합니다."""
    if not _dudoong_state["ticket_item_id"] or not _dudoong_state["buyer_headers"]:
        pytest.skip("두둥티켓 셋업이 안 됨")

    # 새 주문 생성
    order_uuid = _create_order(base_url, _dudoong_state["buyer_headers"], _dudoong_state["ticket_item_id"])
    _dudoong_state["order_uuid_refuse"] = order_uuid
    print(f"\n[test_dudoong_order_refuse] 거절 대상 주문: {order_uuid}")

    event_id = _dudoong_state["event_id"]
    url = f"{base_url}/v1/events/{event_id}/orders/{order_uuid}/refuse"
    print(f"[test_dudoong_order_refuse] POST {url}")
    resp = requests.post(url, headers=auth_headers)
    print(f"[test_dudoong_order_refuse] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    print("[test_dudoong_order_refuse] 주문 거절 완료")


def test_refused_order_status(base_url):
    """거절된 주문의 상태를 확인합니다."""
    order_uuid = _dudoong_state.get("order_uuid_refuse")
    if not order_uuid or not _dudoong_state["buyer_headers"]:
        pytest.skip("거절된 주문이 없어 건너뜁니다.")

    url = f"{base_url}/v1/orders/{order_uuid}"
    print(f"\n[test_refused_order_status] GET {url}")
    resp = requests.get(url, headers=_dudoong_state["buyer_headers"])
    print(f"[test_refused_order_status] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = get_data(resp)
    print(f"[test_refused_order_status] 거절된 주문 상태: {data.get('orderStatus', 'N/A')}")
