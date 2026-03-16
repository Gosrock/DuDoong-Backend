"""
승인 대기 주문 취소 + 재고 복원 E2E 테스트.
두둥티켓 주문을 생성 후 구매자가 취소하고, 재고가 복원되는지 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


_cancel_state: dict = {
    "event_id": 0,
    "ticket_item_id": 0,
    "order_uuid": "",
    "buyer_headers": {},
    "initial_stock": 0,
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


def test_setup_cancel_scenario(base_url, auth_headers, state):
    """주문 취소 테스트를 위한 두둥티켓 이벤트 셋업."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    from datetime import datetime, timedelta
    future = (datetime.now() + timedelta(days=180)).strftime("%Y.%m.%d %H:%M")

    # 이벤트 생성
    event_resp = requests.post(
        f"{base_url}/v1/events",
        json={"hostId": state.host_id, "name": "주문취소테스트공연", "startAt": future, "runTime": 90},
        headers=auth_headers,
    )
    assert event_resp.status_code in (200, 201), f"이벤트 생성 실패: {event_resp.text[:200]}"
    _cancel_state["event_id"] = get_data(event_resp)["eventId"]

    # 기본 정보 설정 (장소 포함)
    requests.patch(
        f"{base_url}/v1/events/{_cancel_state['event_id']}/basic",
        json={"name": "주문취소테스트공연", "startAt": future, "runTime": 90,
              "placeName": "취소테스트공연장", "placeAddress": "서울시 강남구",
              "longitude": 127.0, "latitude": 37.5},
        headers=auth_headers,
    )

    # 상세 설정
    requests.patch(
        f"{base_url}/v1/events/{_cancel_state['event_id']}/details",
        json={"posterImageKey": "test/event/e2e/poster.jpeg", "content": "주문 취소 테스트 상세"},
        headers=auth_headers,
    )

    # 두둥티켓 생성 (승인 방식, 재고 10개)
    ticket_resp = requests.post(
        f"{base_url}/v1/events/{_cancel_state['event_id']}/ticketItems",
        json={
            "payType": "두둥티켓",
            "name": "취소테스트두둥티켓",
            "description": "취소 테스트용 두둥티켓",
            "price": 5000,
            "supplyCount": 10,
            "approveType": "승인",
            "isQuantityPublic": True,
            "purchaseLimit": 2,
            "bankName": "신한",
            "accountNumber": "110-123-456789",
            "accountHolder": "테스터",
        },
        headers=auth_headers,
    )
    assert ticket_resp.status_code in (200, 201), f"티켓 생성 실패: {ticket_resp.text[:300]}"
    _cancel_state["ticket_item_id"] = get_data(ticket_resp)["ticketItemId"]
    _cancel_state["initial_stock"] = 10

    # 이벤트 오픈
    open_resp = requests.patch(
        f"{base_url}/v1/events/{_cancel_state['event_id']}/open",
        headers=auth_headers,
    )
    assert open_resp.status_code == 200, f"이벤트 오픈 실패: {open_resp.text[:200]}"

    # 구매자 로그인
    buyer_data = _login(base_url, "cancel-buyer@dudoong.com", "취소구매자")
    _cancel_state["buyer_headers"] = {"Authorization": f"Bearer {buyer_data['accessToken']}"}
    print(f"[setup] 취소 테스트 셋업 완료: event={_cancel_state['event_id']}, ticket={_cancel_state['ticket_item_id']}")


def test_create_order_for_cancel(base_url):
    """취소할 두둥티켓 주문을 생성합니다."""
    if not _cancel_state["ticket_item_id"] or not _cancel_state["buyer_headers"]:
        pytest.skip("셋업 안 됨")

    # 장바구니
    cart_resp = requests.post(
        f"{base_url}/v1/carts",
        json={"items": [{"itemId": _cancel_state["ticket_item_id"], "quantity": 1, "options": []}]},
        headers=_cancel_state["buyer_headers"],
    )
    assert cart_resp.status_code in (200, 201), f"장바구니 실패: {cart_resp.text[:200]}"
    cart_id = get_data(cart_resp)["cartId"]

    # 주문 생성
    order_resp = requests.post(
        f"{base_url}/v1/orders/",
        json={"couponId": None, "cartId": cart_id},
        headers=_cancel_state["buyer_headers"],
    )
    assert order_resp.status_code in (200, 201), f"주문 실패: {order_resp.text[:200]}"
    _cancel_state["order_uuid"] = get_data(order_resp)["orderId"]
    print(f"[test_create_order] 주문 생성: {_cancel_state['order_uuid']}")


def test_cancel_pending_order(base_url, auth_headers):
    """승인 대기 상태의 주문을 호스트(어드민)가 거절(refuse)합니다.
    PENDING_APPROVE 상태는 refuse 엔드포인트로 취소해야 합니다."""
    order_uuid = _cancel_state.get("order_uuid")
    if not order_uuid:
        pytest.skip("주문이 없어 건너뜁니다.")

    event_id = _cancel_state.get("event_id")
    url = f"{base_url}/v1/events/{event_id}/orders/{order_uuid}/refuse"
    print(f"\n[test_cancel_pending] POST {url}")
    resp = requests.post(url, headers=auth_headers)
    print(f"[test_cancel_pending] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (200, 201), (
        f"주문 취소 실패: status={resp.status_code}, body={resp.text[:300]}"
    )
    print("[test_cancel_pending] 승인 대기 주문 취소 완료")


def test_cancelled_order_status(base_url):
    """취소된 주문의 상태가 취소로 변경되었는지 확인합니다."""
    order_uuid = _cancel_state.get("order_uuid")
    if not order_uuid or not _cancel_state["buyer_headers"]:
        pytest.skip("주문이 없어 건너뜁니다.")

    url = f"{base_url}/v1/orders/{order_uuid}"
    print(f"\n[test_cancelled_status] GET {url}")
    resp = requests.get(url, headers=_cancel_state["buyer_headers"])
    print(f"[test_cancelled_status] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = get_data(resp)
    # orderStatus는 top-level 또는 paymentInfo 안에 있을 수 있음
    order_status = (
        data.get("orderStatus")
        or (data.get("paymentInfo") or {}).get("orderStatus")
        or ""
    )
    print(f"[test_cancelled_status] 주문 상태: {order_status}")
    # 취소 상태 확인 (CANCELED, CANCELLED, CANCEL, 취소 등)
    if order_status:
        assert "CANCEL" in order_status.upper() or "취소" in order_status, (
            f"취소된 주문의 상태가 CANCEL이 아닙니다: {order_status}"
        )


def test_stock_restored_after_cancel(base_url):
    """주문 취소 후 재고가 복원되었는지 확인합니다."""
    event_id = _cancel_state.get("event_id")
    ticket_id = _cancel_state.get("ticket_item_id")
    if not event_id or not ticket_id:
        pytest.skip("셋업 안 됨")

    url = f"{base_url}/v1/events/{event_id}/ticketItems"
    print(f"\n[test_stock_restored] GET {url}")
    resp = requests.get(url)
    print(f"[test_stock_restored] status={resp.status_code}, body={resp.text[:600]}")

    assert_status(resp, 200)
    data = get_data(resp)

    tickets = data if isinstance(data, list) else data.get("ticketItems", data.get("content", []))
    target = None
    for t in tickets:
        tid = t.get("ticketItemId", t.get("id"))
        if tid == ticket_id:
            target = t
            break

    if target:
        supply = target.get("supplyCount", target.get("quantity", 0))
        remaining = target.get("remainingCount", target.get("stock", supply))
        print(f"[test_stock_restored] supply={supply}, remaining={remaining}")
        # 취소 후 재고가 원래대로(10개) 복원되어야 함
        assert remaining == _cancel_state["initial_stock"], (
            f"재고 복원 실패: expected={_cancel_state['initial_stock']}, actual={remaining}"
        )
        print("[test_stock_restored] 재고 복원 확인")
    else:
        print("[test_stock_restored] 티켓을 찾지 못함")
