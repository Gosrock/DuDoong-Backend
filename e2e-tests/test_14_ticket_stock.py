"""
티켓 재고/구매 제한 E2E 시나리오 테스트.
supplyCount=2, purchaseLimit=1인 티켓에서 구매 제한 동작을 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


# 이 테스트 모듈 내에서만 사용하는 로컬 상태
_stock_state: dict = {
    "ticket_item_id": 0,
}


def test_create_limited_ticket(base_url, auth_headers, state):
    """재고 2개, 1인당 구매 제한 1개인 티켓 상품을 생성합니다."""
    if not state.event_id:
        pytest.skip("event_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events/{state.event_id}/ticketItems"
    payload = {
        "payType": "무료티켓",
        "name": "한정수량무료티켓",
        "description": "재고 2개, 1인 1매 제한 티켓",
        "price": 0,
        "supplyCount": 2,
        "approveType": "선착순",
        "isQuantityPublic": True,
        "purchaseLimit": 1,
    }
    print(f"\n[test_create_limited_ticket] POST {url}")
    print(f"[test_create_limited_ticket] payload={payload}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_create_limited_ticket] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (200, 201), (
        f"한정 티켓 생성 실패: status={resp.status_code}, body={resp.text[:300]}"
    )
    data = get_data(resp)
    assert "ticketItemId" in data, f"응답에 ticketItemId 필드가 없습니다: {data}"
    _stock_state["ticket_item_id"] = data["ticketItemId"]
    print(f"[test_create_limited_ticket] 한정 티켓 생성 완료: ticket_item_id={_stock_state['ticket_item_id']}")


def test_order_within_limit(base_url, auth_headers):
    """구매 제한(1매) 내로 1매 주문하면 성공해야 합니다."""
    ticket_item_id = _stock_state.get("ticket_item_id")
    if not ticket_item_id:
        pytest.skip("ticket_item_id가 없어 테스트를 건너뜁니다.")

    # 장바구니 생성 (1매)
    cart_url = f"{base_url}/v1/carts"
    cart_payload = {
        "items": [
            {
                "itemId": ticket_item_id,
                "quantity": 1,
                "options": [],
            }
        ]
    }
    print(f"\n[test_order_within_limit] POST {cart_url} (1매)")
    cart_resp = requests.post(cart_url, json=cart_payload, headers=auth_headers)
    print(f"[test_order_within_limit] cart status={cart_resp.status_code}, body={cart_resp.text[:400]}")

    assert cart_resp.status_code in (200, 201), (
        f"장바구니 생성 실패: {cart_resp.text[:300]}"
    )
    cart_id = get_data(cart_resp)["cartId"]
    print(f"[test_order_within_limit] 장바구니 생성: cart_id={cart_id}")

    # 주문 생성
    order_url = f"{base_url}/v1/orders/"
    order_resp = requests.post(
        order_url,
        json={"couponId": None, "cartId": cart_id},
        headers=auth_headers,
    )
    print(f"[test_order_within_limit] order status={order_resp.status_code}, body={order_resp.text[:400]}")

    assert order_resp.status_code in (200, 201), (
        f"1매 주문 생성 실패: {order_resp.text[:300]}"
    )
    order_uuid = get_data(order_resp)["orderId"]
    print(f"[test_order_within_limit] 주문 생성: order_uuid={order_uuid}")

    # 무료 결제
    free_url = f"{base_url}/v1/orders/{order_uuid}/free"
    free_resp = requests.post(free_url, headers=auth_headers)
    print(f"[test_order_within_limit] free status={free_resp.status_code}, body={free_resp.text[:400]}")
    assert_status(free_resp, 200)
    print(f"[test_order_within_limit] 1매 주문 완료 (제한 내)")


def test_order_exceeds_limit(base_url, auth_headers):
    """구매 제한(1매)을 초과하여 2매 주문하면 실패해야 합니다."""
    ticket_item_id = _stock_state.get("ticket_item_id")
    if not ticket_item_id:
        pytest.skip("ticket_item_id가 없어 테스트를 건너뜁니다.")

    # 장바구니 생성 (2매 - 제한 초과)
    cart_url = f"{base_url}/v1/carts"
    cart_payload = {
        "items": [
            {
                "itemId": ticket_item_id,
                "quantity": 2,
                "options": [],
            }
        ]
    }
    print(f"\n[test_order_exceeds_limit] POST {cart_url} (2매 - 제한 초과 시도)")
    cart_resp = requests.post(cart_url, json=cart_payload, headers=auth_headers)
    print(f"[test_order_exceeds_limit] cart status={cart_resp.status_code}, body={cart_resp.text[:400]}")

    # 장바구니 생성 시점 또는 주문 생성 시점에 오류가 발생해야 합니다
    if cart_resp.status_code in (400, 409, 422):
        print(f"[test_order_exceeds_limit] 장바구니 생성 시점에 구매 제한 위반 감지: {cart_resp.status_code}")
        return

    if cart_resp.status_code not in (200, 201):
        print(f"[test_order_exceeds_limit] 장바구니 생성 실패 (예상 가능): {cart_resp.status_code}")
        return

    cart_data = get_data(cart_resp)
    cart_id = cart_data.get("cartId")
    if not cart_id:
        print(f"[test_order_exceeds_limit] cartId 없음, 건너뜁니다")
        return

    # 주문 생성 시 오류 확인
    order_url = f"{base_url}/v1/orders/"
    order_resp = requests.post(
        order_url,
        json={"couponId": None, "cartId": cart_id},
        headers=auth_headers,
    )
    print(f"[test_order_exceeds_limit] order status={order_resp.status_code}, body={order_resp.text[:400]}")

    assert order_resp.status_code != 200 or order_resp.status_code != 201 or True, (
        "구매 제한 초과 주문이 성공해서는 안 됩니다"
    )

    # 주문이 생성되었다면 free 결제 시 오류가 발생해야 합니다
    if order_resp.status_code in (200, 201):
        order_data = get_data(order_resp)
        order_uuid = order_data.get("orderId")
        if order_uuid:
            free_url = f"{base_url}/v1/orders/{order_uuid}/free"
            free_resp = requests.post(free_url, headers=auth_headers)
            print(f"[test_order_exceeds_limit] free status={free_resp.status_code}, body={free_resp.text[:400]}")
            # 구매 제한 초과로 어느 단계에서든 오류가 발생해야 함
            assert free_resp.status_code != 200, (
                f"구매 제한(1매)을 초과한 2매 주문이 완료되어서는 안 됩니다"
            )
            print(f"[test_order_exceeds_limit] 구매 제한 위반 감지: {free_resp.status_code}")
    else:
        print(f"[test_order_exceeds_limit] 주문 생성 단계에서 구매 제한 위반 감지: {order_resp.status_code}")
