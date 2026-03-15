"""
발급 티켓 관련 E2E 시나리오 테스트.
주문 완료 후 발급된 티켓 목록 및 상세 조회를 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data, get_data


def test_read_order_tickets(base_url, auth_headers, state):
    """
    완료된 주문에서 발급된 티켓 목록을 조회합니다.
    주문 완료 후 티켓이 정상적으로 발급되었는지 확인합니다.
    """
    assert state.order_uuid, "order_uuid가 없습니다. test_05_order_flow를 먼저 실행하세요."
    url = f"{base_url}/v1/orders/{state.order_uuid}/tickets"
    print(f"\n[test_read_order_tickets] GET {url}")
    resp = requests.get(url, headers=auth_headers)
    print(f"[test_read_order_tickets] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = get_data(resp)
    # OrderTicketResponse 구조 확인
    print(f"[test_read_order_tickets] 주문 티켓 조회 완료: {data}")


def test_read_my_orders(base_url, auth_headers, state):
    """
    마이페이지 예매 목록에서 완료된 주문이 조회되는지 확인합니다.
    showing=true 파라미터로 현재 유효한 예매 목록을 조회합니다.
    """
    url = f"{base_url}/v1/orders"
    params = {"showing": "true"}
    print(f"\n[test_read_my_orders] GET {url} params={params}")
    resp = requests.get(url, params=params, headers=auth_headers)
    print(f"[test_read_my_orders] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = get_data(resp)
    assert "content" in data, f"응답에 content 필드가 없습니다: {data}"
    print(f"[test_read_my_orders] 예매 목록 조회 완료: {len(data['content'])}개")
    if state.order_uuid:
        uuids = [o.get("orderId") for o in data["content"]]
        print(f"[test_read_my_orders] 조회된 order UUIDs: {uuids}")
