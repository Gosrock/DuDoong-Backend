"""
관리자(호스트) 기능 E2E 테스트.
관리자 주문 테이블 조회, 발급 티켓 테이블 조회, 입장 처리, 이중 입장 방지를 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


# 이 모듈 내에서만 사용하는 로컬 상태
_admin_state: dict = {
    "issued_ticket_uuid": "",
}


def test_admin_order_table(base_url, auth_headers, state):
    """관리자가 이벤트의 주문 테이블을 조회합니다."""
    if not state.event_id:
        pytest.skip("event_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events/{state.event_id}/orders"
    params = {"orderStage": "ALL", "page": 0, "size": 10}
    print(f"\n[test_admin_order_table] GET {url} params={params}")
    resp = requests.get(url, params=params, headers=auth_headers)
    print(f"[test_admin_order_table] status={resp.status_code}, body={resp.text[:500]}")

    assert_status(resp, 200)
    data = get_data(resp)
    assert "content" in data, f"content 필드 없음: {data}"
    print(f"[test_admin_order_table] 관리자 주문 목록 조회: {len(data['content'])}건")


def test_admin_issued_ticket_table(base_url, auth_headers, state):
    """관리자가 이벤트의 발급 티켓 테이블을 조회합니다."""
    if not state.event_id:
        pytest.skip("event_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events/{state.event_id}/issuedTickets"
    params = {"page": 0, "size": 10}
    print(f"\n[test_admin_issued_ticket_table] GET {url}")
    resp = requests.get(url, params=params, headers=auth_headers)
    print(f"[test_admin_issued_ticket_table] status={resp.status_code}, body={resp.text[:500]}")

    assert_status(resp, 200)
    data = get_data(resp)
    assert "content" in data, f"content 필드 없음: {data}"
    print(f"[test_admin_issued_ticket_table] 발급 티켓 목록: {len(data['content'])}건")

    # 입장 처리 테스트를 위해 첫 번째 티켓 UUID 저장
    if data["content"]:
        first_ticket = data["content"][0]
        uuid_key = "issuedTicketNo" if "issuedTicketNo" in first_ticket else "uuid"
        _admin_state["issued_ticket_uuid"] = first_ticket.get(uuid_key, "")
        print(f"[test_admin_issued_ticket_table] 입장 처리 대상: {_admin_state['issued_ticket_uuid']}")


def test_entry_check_in(base_url, auth_headers, state):
    """관리자가 발급 티켓에 입장 처리를 합니다."""
    if not state.event_id:
        pytest.skip("event_id가 없어 테스트를 건너뜁니다.")
    ticket_uuid = _admin_state.get("issued_ticket_uuid")
    if not ticket_uuid:
        pytest.skip("입장 처리할 티켓 UUID가 없어 건너뜁니다.")

    url = f"{base_url}/v1/events/{state.event_id}/issuedTickets/{ticket_uuid}"
    print(f"\n[test_entry_check_in] PATCH {url}")
    resp = requests.patch(url, headers=auth_headers)
    print(f"[test_entry_check_in] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    print("[test_entry_check_in] 입장 처리 완료")


def test_double_entry_fails(base_url, auth_headers, state):
    """이미 입장한 티켓을 재입장 처리하면 실패합니다."""
    if not state.event_id:
        pytest.skip("event_id가 없어 테스트를 건너뜁니다.")
    ticket_uuid = _admin_state.get("issued_ticket_uuid")
    if not ticket_uuid:
        pytest.skip("입장 처리할 티켓 UUID가 없어 건너뜁니다.")

    url = f"{base_url}/v1/events/{state.event_id}/issuedTickets/{ticket_uuid}"
    print(f"\n[test_double_entry_fails] PATCH {url} (이중 입장 시도)")
    resp = requests.patch(url, headers=auth_headers)
    print(f"[test_double_entry_fails] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code != 200, (
        f"이미 입장한 티켓의 재입장이 성공해서는 안 됩니다: {resp.status_code}"
    )
    print(f"[test_double_entry_fails] 이중 입장 차단 확인: {resp.status_code}")


def test_admin_cancel_order(base_url, auth_headers, state):
    """관리자가 주문을 취소합니다."""
    if not state.event_id or not state.ticket_item_id:
        pytest.skip("event_id 또는 ticket_item_id 없음")

    # 취소 테스트용 새 주문 생성
    cart_resp = requests.post(
        f"{base_url}/v1/carts",
        json={"items": [{"itemId": state.ticket_item_id, "quantity": 1, "options": []}]},
        headers=auth_headers,
    )
    if cart_resp.status_code not in (200, 201):
        pytest.skip(f"장바구니 생성 실패: {cart_resp.text[:200]}")
    cart_id = get_data(cart_resp)["cartId"]

    order_resp = requests.post(
        f"{base_url}/v1/orders/",
        json={"couponId": None, "cartId": cart_id},
        headers=auth_headers,
    )
    if order_resp.status_code not in (200, 201):
        pytest.skip(f"주문 생성 실패: {order_resp.text[:200]}")
    order_uuid = get_data(order_resp)["orderId"]

    # 무료 결제
    free_resp = requests.post(f"{base_url}/v1/orders/{order_uuid}/free", headers=auth_headers)
    if free_resp.status_code != 200:
        pytest.skip(f"무료결제 실패: {free_resp.text[:200]}")

    # 관리자 취소
    cancel_url = f"{base_url}/v1/events/{state.event_id}/orders/{order_uuid}/cancel"
    print(f"\n[test_admin_cancel_order] POST {cancel_url}")
    cancel_resp = requests.post(cancel_url, headers=auth_headers)
    print(f"[test_admin_cancel_order] status={cancel_resp.status_code}, body={cancel_resp.text[:400]}")

    assert_status(cancel_resp, 200)
    print("[test_admin_cancel_order] 관리자 주문 취소 완료")
