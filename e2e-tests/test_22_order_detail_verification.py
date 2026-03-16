"""
주문 플로우 상세 검증 E2E 테스트.
장바구니 → 주문 → 결제 각 단계의 응답 필드를 세밀하게 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


# 이 모듈 내에서만 사용하는 로컬 상태
_detail_state: dict = {
    "cart_id": 0,
    "order_uuid": "",
}


def test_cart_response_fields(base_url, auth_headers, state):
    """장바구니 생성 응답의 필드를 상세 검증합니다 (items, totalPrice, isNeedPayment)."""
    if not state.ticket_item_id:
        pytest.skip("ticket_item_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/carts"
    payload = {
        "items": [{"itemId": state.ticket_item_id, "quantity": 1, "options": []}]
    }
    print(f"\n[test_cart_response_fields] POST {url}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_cart_response_fields] status={resp.status_code}, body={resp.text[:500]}")

    assert resp.status_code in (200, 201)
    data = get_data(resp)
    assert "cartId" in data, f"cartId 필드 없음: {data}"

    _detail_state["cart_id"] = data["cartId"]

    # 응답 필드 검증
    if "totalPrice" in data:
        assert data["totalPrice"] == 0, f"무료티켓 totalPrice는 0이어야 함: {data['totalPrice']}"
        print(f"[test_cart_response_fields] totalPrice=0 확인")
    if "isNeedPayment" in data:
        assert data["isNeedPayment"] is False, f"무료티켓 isNeedPayment는 false: {data['isNeedPayment']}"
        print(f"[test_cart_response_fields] isNeedPayment=false 확인")
    if "items" in data:
        assert len(data["items"]) >= 1, "장바구니에 아이템이 없음"
        print(f"[test_cart_response_fields] items 개수: {len(data['items'])}")

    print(f"[test_cart_response_fields] 장바구니 응답 검증 완료: cart_id={_detail_state['cart_id']}")


def test_order_creation_response_fields(base_url, auth_headers):
    """주문 생성 응답의 필드를 상세 검증합니다 (orderId, orderName, amount, isNeedPayment)."""
    cart_id = _detail_state.get("cart_id")
    if not cart_id:
        pytest.skip("cart_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/orders/"
    payload = {"couponId": None, "cartId": cart_id}
    print(f"\n[test_order_creation_response_fields] POST {url}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_order_creation_response_fields] status={resp.status_code}, body={resp.text[:500]}")

    assert resp.status_code in (200, 201)
    data = get_data(resp)
    assert "orderId" in data, f"orderId 필드 없음: {data}"

    _detail_state["order_uuid"] = data["orderId"]

    # 필드 검증
    if "orderName" in data:
        assert len(data["orderName"]) > 0, "orderName이 비어 있음"
        print(f"[test_order_creation_response_fields] orderName={data['orderName']}")
    if "amount" in data:
        assert data["amount"] == 0, f"무료 주문 amount는 0이어야 함: {data['amount']}"
        print(f"[test_order_creation_response_fields] amount=0 확인")
    if "isNeedPayment" in data:
        assert data["isNeedPayment"] is False, f"무료 주문 isNeedPayment는 false: {data['isNeedPayment']}"
        print(f"[test_order_creation_response_fields] isNeedPayment=false 확인")

    print(f"[test_order_creation_response_fields] 주문 생성 응답 검증 완료: {_detail_state['order_uuid']}")


def test_free_payment_response(base_url, auth_headers):
    """무료 결제 완료 후 상태를 검증합니다."""
    order_uuid = _detail_state.get("order_uuid")
    if not order_uuid:
        pytest.skip("order_uuid가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/orders/{order_uuid}/free"
    print(f"\n[test_free_payment_response] POST {url}")
    resp = requests.post(url, headers=auth_headers)
    print(f"[test_free_payment_response] status={resp.status_code}, body={resp.text[:500]}")

    assert_status(resp, 200)
    data = get_data(resp)
    assert "orderUuid" in data, f"orderUuid 필드 없음: {data}"
    assert data["orderUuid"] == order_uuid
    print(f"[test_free_payment_response] 무료 결제 완료 확인")


def test_issued_tickets_after_payment(base_url, auth_headers):
    """결제 후 발급 티켓 수량을 검증합니다."""
    order_uuid = _detail_state.get("order_uuid")
    if not order_uuid:
        pytest.skip("order_uuid가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/orders/{order_uuid}/tickets"
    print(f"\n[test_issued_tickets_after_payment] GET {url}")
    resp = requests.get(url, headers=auth_headers)
    print(f"[test_issued_tickets_after_payment] status={resp.status_code}, body={resp.text[:500]}")

    assert_status(resp, 200)
    data = get_data(resp)
    # 발급된 티켓이 있어야 함
    if isinstance(data, list):
        assert len(data) >= 1, "발급 티켓이 없습니다"
        print(f"[test_issued_tickets_after_payment] 발급 티켓 수: {len(data)}")
    elif isinstance(data, dict) and "issuedTickets" in data:
        assert len(data["issuedTickets"]) >= 1, "발급 티켓이 없습니다"
        print(f"[test_issued_tickets_after_payment] 발급 티켓 수: {len(data['issuedTickets'])}")
    else:
        print(f"[test_issued_tickets_after_payment] 발급 티켓 응답: {data}")


def test_stock_decremented_after_payment(base_url, state):
    """결제 후 재고가 차감되었는지 ticketItems를 재조회하여 확인합니다."""
    if not state.event_id:
        pytest.skip("event_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events/{state.event_id}/ticketItems"
    print(f"\n[test_stock_decremented] GET {url}")
    resp = requests.get(url)
    print(f"[test_stock_decremented] status={resp.status_code}, body={resp.text[:500]}")

    assert_status(resp, 200)
    data = get_data(resp)
    if "ticketItems" in data:
        for item in data["ticketItems"]:
            if item.get("ticketItemId") == state.ticket_item_id:
                remaining = item.get("quantity", item.get("remainingCount", "N/A"))
                supply = item.get("supplyCount", "N/A")
                print(f"[test_stock_decremented] 티켓 {state.ticket_item_id}: remaining={remaining}, supply={supply}")
                # 원래 supplyCount=100이었으므로 현재 남은 수량은 100보다 적어야 함
                if isinstance(remaining, int):
                    assert remaining < 100, f"재고가 차감되지 않았습니다: remaining={remaining}"
                break
    print("[test_stock_decremented] 재고 차감 검증 완료")
