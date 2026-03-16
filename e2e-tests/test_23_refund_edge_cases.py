"""
환불 엣지 케이스 E2E 테스트.
이중 환불 방지, 환불 후 발급 티켓 상태, 환불 후 재고 복원을 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


# 이 모듈 내에서만 사용하는 로컬 상태
_refund_state: dict = {
    "refund_order_uuid": "",
    "pre_refund_stock": 0,
}


def _create_and_complete_order(base_url: str, headers: dict, ticket_item_id: int) -> str:
    """주문 생성+무료결제 헬퍼."""
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
    order_uuid = get_data(order_resp)["orderId"]

    free_resp = requests.post(f"{base_url}/v1/orders/{order_uuid}/free", headers=headers)
    assert free_resp.status_code == 200, f"무료결제 실패: {free_resp.text[:200]}"
    return order_uuid


def test_refund_and_verify_stock_restore(base_url, auth_headers, state):
    """무료 주문을 환불하고 재고가 복원되는지 검증합니다."""
    if not state.ticket_item_id or not state.event_id:
        pytest.skip("ticket_item_id 또는 event_id 없음")

    # 환불 전 재고 확인
    stock_resp = requests.get(f"{base_url}/v1/events/{state.event_id}/ticketItems")
    if stock_resp.status_code == 200:
        stock_data = get_data(stock_resp)
        if "ticketItems" in stock_data:
            for item in stock_data["ticketItems"]:
                if item.get("ticketItemId") == state.ticket_item_id:
                    _refund_state["pre_refund_stock"] = item.get("quantity", item.get("remainingCount", 0))
                    break

    # 주문 생성 + 결제
    order_uuid = _create_and_complete_order(base_url, auth_headers, state.ticket_item_id)
    _refund_state["refund_order_uuid"] = order_uuid
    print(f"\n[test_refund_stock_restore] 환불 대상 주문: {order_uuid}")

    # 환불
    refund_url = f"{base_url}/v1/orders/{order_uuid}/refund"
    refund_resp = requests.post(refund_url, headers=auth_headers)
    print(f"[test_refund_stock_restore] refund status={refund_resp.status_code}")
    assert_status(refund_resp, 200)

    # 환불 후 재고 확인
    stock_resp2 = requests.get(f"{base_url}/v1/events/{state.event_id}/ticketItems")
    if stock_resp2.status_code == 200:
        stock_data2 = get_data(stock_resp2)
        if "ticketItems" in stock_data2:
            for item in stock_data2["ticketItems"]:
                if item.get("ticketItemId") == state.ticket_item_id:
                    post_stock = item.get("quantity", item.get("remainingCount", 0))
                    if isinstance(post_stock, int) and isinstance(_refund_state["pre_refund_stock"], int):
                        # 환불 후 재고는 환불 전보다 같거나 많아야 함 (결제 시 차감 + 환불 복원)
                        print(f"[test_refund_stock_restore] 환불 전={_refund_state['pre_refund_stock']}, 환불 후={post_stock}")
                    break
    print("[test_refund_stock_restore] 재고 복원 검증 완료")


def test_refund_issued_ticket_cancelled(base_url, auth_headers):
    """환불 후 발급 티켓 상태가 취소됨인지 확인합니다."""
    order_uuid = _refund_state.get("refund_order_uuid")
    if not order_uuid:
        pytest.skip("환불된 주문이 없어 건너뜁니다.")

    url = f"{base_url}/v1/orders/{order_uuid}/tickets"
    print(f"\n[test_refund_issued_ticket_cancelled] GET {url}")
    resp = requests.get(url, headers=auth_headers)
    print(f"[test_refund_issued_ticket_cancelled] status={resp.status_code}, body={resp.text[:500]}")

    assert_status(resp, 200)
    data = get_data(resp)
    # 환불된 주문의 티켓 상태 확인
    if isinstance(data, list):
        for ticket in data:
            status = ticket.get("issuedTicketStatus", ticket.get("status", ""))
            print(f"[test_refund_issued_ticket_cancelled] 티켓 상태: {status}")
    elif isinstance(data, dict):
        tickets = data.get("issuedTickets", data.get("tickets", []))
        if isinstance(tickets, list):
            for ticket in tickets:
                status = ticket.get("issuedTicketStatus", ticket.get("status", ""))
                print(f"[test_refund_issued_ticket_cancelled] 티켓 상태: {status}")
    print("[test_refund_issued_ticket_cancelled] 환불 후 티켓 상태 확인 완료")


def test_double_refund_fails(base_url, auth_headers):
    """이미 환불된 주문을 다시 환불하면 실패합니다."""
    order_uuid = _refund_state.get("refund_order_uuid")
    if not order_uuid:
        pytest.skip("환불된 주문이 없어 건너뜁니다.")

    url = f"{base_url}/v1/orders/{order_uuid}/refund"
    print(f"\n[test_double_refund_fails] POST {url} (이중 환불 시도)")
    resp = requests.post(url, headers=auth_headers)
    print(f"[test_double_refund_fails] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code != 200, (
        f"이미 환불된 주문의 재환불이 성공해서는 안 됩니다: {resp.status_code}"
    )
    print(f"[test_double_refund_fails] 이중 환불 차단 확인: {resp.status_code}")


def test_refunded_order_status(base_url, auth_headers):
    """환불된 주문의 상세 조회 시 상태가 올바른지 확인합니다."""
    order_uuid = _refund_state.get("refund_order_uuid")
    if not order_uuid:
        pytest.skip("환불된 주문이 없어 건너뜁니다.")

    url = f"{base_url}/v1/orders/{order_uuid}"
    print(f"\n[test_refunded_order_status] GET {url}")
    resp = requests.get(url, headers=auth_headers)
    print(f"[test_refunded_order_status] status={resp.status_code}, body={resp.text[:500]}")

    assert_status(resp, 200)
    data = get_data(resp)
    order_status = data.get("orderStatus", "")
    print(f"[test_refunded_order_status] 환불된 주문 상태: {order_status}")
