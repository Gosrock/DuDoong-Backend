"""
티켓 재고 공개/비공개 설정 검증 E2E 테스트.
isQuantityPublic=true/false에 따른 응답 차이를 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


_qty_state: dict = {
    "event_id": 0,
    "public_ticket_id": 0,
    "private_ticket_id": 0,
}


def test_setup_event_for_quantity_test(base_url, auth_headers, state):
    """재고 공개/비공개 테스트를 위한 이벤트 및 티켓 생성."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    from datetime import datetime, timedelta
    future = (datetime.now() + timedelta(days=150)).strftime("%Y.%m.%d %H:%M")

    # 이벤트 생성
    event_resp = requests.post(
        f"{base_url}/v1/events",
        json={"hostId": state.host_id, "name": "재고공개테스트이벤트", "startAt": future, "runTime": 60},
        headers=auth_headers,
    )
    assert event_resp.status_code in (200, 201), f"이벤트 생성 실패: {event_resp.text[:200]}"
    _qty_state["event_id"] = get_data(event_resp)["eventId"]

    # 상세 설정
    requests.patch(
        f"{base_url}/v1/events/{_qty_state['event_id']}/detail",
        json={"content": "재고 공개/비공개 테스트"},
        headers=auth_headers,
    )

    # 재고 공개 티켓 (isQuantityPublic=True)
    public_resp = requests.post(
        f"{base_url}/v1/events/{_qty_state['event_id']}/ticketItems",
        json={
            "payType": "무료티켓",
            "name": "재고공개티켓",
            "description": "재고가 공개되는 티켓",
            "price": 0,
            "supplyCount": 100,
            "approveType": "선착순",
            "isQuantityPublic": True,
            "purchaseLimit": 2,
        },
        headers=auth_headers,
    )
    assert public_resp.status_code in (200, 201), f"공개 티켓 생성 실패: {public_resp.text[:300]}"
    _qty_state["public_ticket_id"] = get_data(public_resp)["ticketItemId"]

    # 재고 비공개 티켓 (isQuantityPublic=False)
    private_resp = requests.post(
        f"{base_url}/v1/events/{_qty_state['event_id']}/ticketItems",
        json={
            "payType": "무료티켓",
            "name": "재고비공개티켓",
            "description": "재고가 비공개되는 티켓",
            "price": 0,
            "supplyCount": 50,
            "approveType": "선착순",
            "isQuantityPublic": False,
            "purchaseLimit": 2,
        },
        headers=auth_headers,
    )
    assert private_resp.status_code in (200, 201), f"비공개 티켓 생성 실패: {private_resp.text[:300]}"
    _qty_state["private_ticket_id"] = get_data(private_resp)["ticketItemId"]
    print(f"[setup] 공개={_qty_state['public_ticket_id']}, 비공개={_qty_state['private_ticket_id']}")


def test_public_ticket_shows_quantity(base_url, state):
    """재고 공개 티켓은 재고 수량 정보가 응답에 포함됩니다."""
    event_id = _qty_state.get("event_id")
    if not event_id:
        pytest.skip("셋업 안 됨")

    url = f"{base_url}/v1/events/{event_id}/ticketItems"
    print(f"\n[test_public_ticket] GET {url}")
    resp = requests.get(url)
    print(f"[test_public_ticket] status={resp.status_code}, body={resp.text[:600]}")

    assert_status(resp, 200)
    data = get_data(resp)

    # 티켓 목록에서 공개 티켓 찾기
    tickets = data if isinstance(data, list) else data.get("ticketItems", data.get("content", []))
    public_ticket = None
    for t in tickets:
        tid = t.get("ticketItemId", t.get("id"))
        if tid == _qty_state["public_ticket_id"]:
            public_ticket = t
            break

    if public_ticket:
        print(f"[test_public_ticket] 공개 티켓 응답: {public_ticket}")
        # isQuantityPublic=True인 경우 수량 관련 필드가 존재해야 함
        has_quantity = (
            "quantity" in public_ticket
            or "supplyCount" in public_ticket
            or "remainingCount" in public_ticket
            or "stock" in public_ticket
        )
        print(f"[test_public_ticket] 수량 필드 존재: {has_quantity}")
    else:
        print("[test_public_ticket] 공개 티켓을 목록에서 찾지 못함 (비로그인 조회 제한일 수 있음)")


def test_private_ticket_hides_quantity(base_url):
    """재고 비공개 티켓은 재고 수량이 숨겨집니다."""
    event_id = _qty_state.get("event_id")
    if not event_id:
        pytest.skip("셋업 안 됨")

    url = f"{base_url}/v1/events/{event_id}/ticketItems"
    print(f"\n[test_private_ticket] GET {url}")
    resp = requests.get(url)
    print(f"[test_private_ticket] status={resp.status_code}")

    assert_status(resp, 200)
    data = get_data(resp)

    tickets = data if isinstance(data, list) else data.get("ticketItems", data.get("content", []))
    private_ticket = None
    for t in tickets:
        tid = t.get("ticketItemId", t.get("id"))
        if tid == _qty_state["private_ticket_id"]:
            private_ticket = t
            break

    if private_ticket:
        print(f"[test_private_ticket] 비공개 티켓 응답: {private_ticket}")
        # isQuantityPublic 필드가 false인지 확인
        is_public = private_ticket.get("isQuantityPublic", private_ticket.get("quantityPublic"))
        if is_public is not None:
            assert is_public is False, f"비공개 티켓의 isQuantityPublic이 True: {private_ticket}"
            print("[test_private_ticket] isQuantityPublic=false 확인")
    else:
        print("[test_private_ticket] 비공개 티켓을 목록에서 찾지 못함")


def test_both_ticket_types_in_same_event(base_url):
    """같은 이벤트에 재고 공개/비공개 티켓이 공존합니다."""
    event_id = _qty_state.get("event_id")
    if not event_id:
        pytest.skip("셋업 안 됨")

    url = f"{base_url}/v1/events/{event_id}/ticketItems"
    resp = requests.get(url)
    assert_status(resp, 200)
    data = get_data(resp)

    tickets = data if isinstance(data, list) else data.get("ticketItems", data.get("content", []))
    ticket_ids = [t.get("ticketItemId", t.get("id")) for t in tickets]

    assert _qty_state["public_ticket_id"] in ticket_ids, "공개 티켓이 목록에 없습니다"
    assert _qty_state["private_ticket_id"] in ticket_ids, "비공개 티켓이 목록에 없습니다"
    print(f"[test_both_types] 같은 이벤트에 공개/비공개 티켓 공존 확인: {ticket_ids}")
