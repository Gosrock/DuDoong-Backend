"""
환불 E2E 시나리오 테스트.
새로운 주문을 생성한 후 환불 처리까지의 전체 플로우를 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data, get_data


def _create_new_order_for_refund(base_url, auth_headers, state) -> str:
    """
    환불 테스트 전용 새 주문을 생성합니다.
    cart 생성 → order 생성 → free 결제 순서로 진행하고 order_uuid를 반환합니다.
    """
    assert state.ticket_item_id, "ticket_item_id가 없습니다. test_04를 먼저 실행하세요."

    # 1) 장바구니 생성
    cart_resp = requests.post(
        f"{base_url}/v1/carts",
        json={
            "items": [
                {
                    "itemId": state.ticket_item_id,
                    "quantity": 1,
                    "options": [],
                }
            ]
        },
        headers=auth_headers,
    )
    print(f"[refund setup] cart status={cart_resp.status_code}, body={cart_resp.text[:300]}")
    assert cart_resp.status_code in (200, 201), f"환불용 장바구니 생성 실패: {cart_resp.text[:200]}"
    cart_id = get_data(cart_resp)["cartId"]

    # 2) 주문 생성
    order_resp = requests.post(
        f"{base_url}/v1/orders/",
        json={"couponId": None, "cartId": cart_id},
        headers=auth_headers,
    )
    print(f"[refund setup] order status={order_resp.status_code}, body={order_resp.text[:300]}")
    assert order_resp.status_code in (200, 201), f"환불용 주문 생성 실패: {order_resp.text[:200]}"
    order_uuid = get_data(order_resp)["orderId"]

    # 3) 무료 결제 완료
    free_resp = requests.post(
        f"{base_url}/v1/orders/{order_uuid}/free",
        headers=auth_headers,
    )
    print(f"[refund setup] free status={free_resp.status_code}, body={free_resp.text[:300]}")
    assert free_resp.status_code == 200, f"환불용 무료 결제 실패: {free_resp.text[:200]}"

    return order_uuid


def test_refund_order(base_url, auth_headers, state):
    """
    새로운 주문을 생성하고 완료 처리한 뒤 환불을 요청합니다.
    환불 후 응답에 orderId가 포함되는지 확인합니다.
    """
    print(f"\n[test_refund_order] 환불 테스트용 주문 생성 중...")
    refund_order_uuid = _create_new_order_for_refund(base_url, auth_headers, state)
    state.refund_order_uuid = refund_order_uuid
    print(f"[test_refund_order] 환불 대상 주문: {refund_order_uuid}")

    url = f"{base_url}/v1/orders/{refund_order_uuid}/refund"
    print(f"[test_refund_order] POST {url}")
    resp = requests.post(url, headers=auth_headers)
    print(f"[test_refund_order] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = get_data(resp)
    assert "orderUuid" in data, f"응답에 orderUuid 필드가 없습니다: {data}"
    assert data["orderUuid"] == refund_order_uuid, "환불된 orderUuid가 일치하지 않습니다"
    print(f"[test_refund_order] 환불 완료: orderUuid={data.get('orderUuid')}")
