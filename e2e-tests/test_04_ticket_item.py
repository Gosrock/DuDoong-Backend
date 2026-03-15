"""
티켓 상품 관련 E2E 시나리오 테스트.
무료 티켓 생성 및 이벤트의 티켓 목록 조회를 검증합니다.
"""
import pytest
import requests

from conftest import assert_status


def test_create_free_ticket_item(base_url, auth_headers, state):
    """
    이벤트에 무료 티켓 상품을 생성하고 ticket_item_id를 state에 저장합니다.
    payType=무료티켓, approveType=선착순으로 생성합니다.
    """
    assert state.event_id, "event_id가 없습니다. test_03_event를 먼저 실행하세요."
    url = f"{base_url}/v1/events/{state.event_id}/ticketItems"
    payload = {
        "payType": "무료티켓",
        "name": "E2E무료티켓",
        "description": "E2E 테스트용 무료 입장 티켓입니다.",
        "price": 0,
        "supplyCount": 100,
        "approveType": "선착순",
        "isQuantityPublic": True,
        "purchaseLimit": 2,
    }
    print(f"\n[test_create_free_ticket_item] POST {url}")
    print(f"[test_create_free_ticket_item] payload={payload}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_create_free_ticket_item] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (200, 201), (
        f"티켓 상품 생성 실패: status={resp.status_code}, body={resp.text[:300]}"
    )
    data = resp.json()
    assert "id" in data, f"응답에 id 필드가 없습니다: {data}"
    state.ticket_item_id = data["id"]
    print(f"[test_create_free_ticket_item] 티켓 상품 생성 완료: ticket_item_id={state.ticket_item_id}")


def test_get_event_ticket_items(base_url, state):
    """
    인증 없이 이벤트의 티켓 상품 목록을 조회합니다.
    생성된 티켓 상품이 목록에 포함되는지 확인합니다.
    """
    assert state.event_id, "event_id가 없습니다."
    url = f"{base_url}/v1/events/{state.event_id}/ticketItems"
    print(f"\n[test_get_event_ticket_items] GET {url} (인증 없음)")
    resp = requests.get(url)
    print(f"[test_get_event_ticket_items] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = resp.json()
    # GetEventTicketItemsResponse 구조: {"ticketItems": [...]}
    assert "ticketItems" in data, f"응답에 ticketItems 필드가 없습니다: {data}"
    items = data["ticketItems"]
    print(f"[test_get_event_ticket_items] 티켓 상품 목록 조회 완료: {len(items)}개")
    if state.ticket_item_id:
        ids = [t.get("id") for t in items]
        assert state.ticket_item_id in ids, (
            f"생성된 ticket_item_id={state.ticket_item_id}가 목록에 없습니다: {ids}"
        )
        print(f"[test_get_event_ticket_items] 생성된 티켓 상품 확인됨: id={state.ticket_item_id}")
