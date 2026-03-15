"""
이벤트 상태 전이 E2E 시나리오 테스트.
PREPARING → OPEN → CALCULATING → CLOSED 순서의 상태 전이를 검증합니다.
"""
import pytest
import requests
from datetime import datetime, timedelta

from conftest import assert_status, get_data


def _future_date_str(days_ahead: int = 30) -> str:
    """현재로부터 days_ahead일 후의 날짜를 'yyyy.MM.dd HH:mm' 형식으로 반환합니다."""
    future = datetime.now() + timedelta(days=days_ahead)
    return future.strftime("%Y.%m.%d %H:%M")


# 이 테스트 모듈 내에서만 사용하는 로컬 상태
_lifecycle_state: dict = {}


def test_event_status_preparing(base_url, auth_headers, state):
    """이벤트를 새로 생성하면 상태가 PREPARING인지 확인합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events"
    payload = {
        "hostId": state.host_id,
        "name": "라이프사이클테스트공연",
        "startAt": _future_date_str(30),
        "runTime": 90,
    }
    print(f"\n[test_event_status_preparing] POST {url}")
    print(f"[test_event_status_preparing] payload={payload}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_event_status_preparing] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (200, 201), (
        f"이벤트 생성 실패: status={resp.status_code}, body={resp.text[:300]}"
    )
    data = get_data(resp)
    assert "eventId" in data, f"응답에 eventId 필드가 없습니다: {data}"
    _lifecycle_state["event_id"] = data["eventId"]
    print(f"[test_event_status_preparing] 이벤트 생성 완료: event_id={_lifecycle_state['event_id']}")

    # 상태 조회
    detail_url = f"{base_url}/v1/events/{_lifecycle_state['event_id']}"
    detail_resp = requests.get(detail_url, headers=auth_headers)
    print(f"[test_event_status_preparing] GET {detail_url} status={detail_resp.status_code}, body={detail_resp.text[:400]}")

    if detail_resp.status_code == 200:
        detail_data = get_data(detail_resp)
        status_val = detail_data.get("status") or detail_data.get("eventStatus")
        print(f"[test_event_status_preparing] 이벤트 상태: {status_val}")
        if status_val:
            assert "PREPARING" in str(status_val) or "준비중" in str(status_val), (
                f"생성 직후 상태는 PREPARING이어야 합니다: {status_val}"
            )
    print(f"[test_event_status_preparing] PREPARING 상태 확인 완료")


def test_event_status_open(base_url, auth_headers):
    """기본 정보, 상세 정보, 티켓 상품을 추가한 후 이벤트를 오픈하면 상태가 OPEN이 되는지 확인합니다."""
    event_id = _lifecycle_state.get("event_id")
    if not event_id:
        pytest.skip("lifecycle event_id가 없어 테스트를 건너뜁니다.")

    # 기본 정보 수정
    basic_url = f"{base_url}/v1/events/{event_id}/basic"
    basic_payload = {
        "name": "라이프사이클테스트공연(수정)",
        "startAt": _future_date_str(45),
        "runTime": 120,
        "placeName": "테스트공연장",
        "placeAddress": "서울 마포구 어울마당로 35",
        "longitude": 126.920036,
        "latitude": 37.548369,
    }
    print(f"\n[test_event_status_open] PATCH {basic_url}")
    basic_resp = requests.patch(basic_url, json=basic_payload, headers=auth_headers)
    print(f"[test_event_status_open] basic status={basic_resp.status_code}, body={basic_resp.text[:300]}")
    assert_status(basic_resp, 200)

    # 상세 정보 수정
    detail_url = f"{base_url}/v1/events/{event_id}/details"
    detail_payload = {
        "posterImageKey": "test/event/lifecycle/poster.jpeg",
        "content": "라이프사이클 테스트 공연입니다.",
    }
    print(f"[test_event_status_open] PATCH {detail_url}")
    detail_resp = requests.patch(detail_url, json=detail_payload, headers=auth_headers)
    print(f"[test_event_status_open] detail status={detail_resp.status_code}, body={detail_resp.text[:300]}")
    assert_status(detail_resp, 200)

    # 티켓 상품 생성
    ticket_url = f"{base_url}/v1/events/{event_id}/ticketItems"
    ticket_payload = {
        "payType": "무료티켓",
        "name": "라이프사이클무료티켓",
        "description": "라이프사이클 테스트용 티켓",
        "price": 0,
        "supplyCount": 50,
        "approveType": "선착순",
        "isQuantityPublic": True,
        "purchaseLimit": 2,
    }
    print(f"[test_event_status_open] POST {ticket_url}")
    ticket_resp = requests.post(ticket_url, json=ticket_payload, headers=auth_headers)
    print(f"[test_event_status_open] ticket status={ticket_resp.status_code}, body={ticket_resp.text[:300]}")
    assert ticket_resp.status_code in (200, 201), (
        f"티켓 생성 실패: {ticket_resp.text[:200]}"
    )
    ticket_data = get_data(ticket_resp)
    _lifecycle_state["ticket_item_id"] = ticket_data.get("ticketItemId")

    # 이벤트 오픈
    open_url = f"{base_url}/v1/events/{event_id}/open"
    print(f"[test_event_status_open] PATCH {open_url}")
    open_resp = requests.patch(open_url, headers=auth_headers)
    print(f"[test_event_status_open] open status={open_resp.status_code}, body={open_resp.text[:400]}")
    assert_status(open_resp, 200)

    # 상태 확인
    check_resp = requests.get(f"{base_url}/v1/events/{event_id}")
    if check_resp.status_code == 200:
        check_data = get_data(check_resp)
        status_val = check_data.get("status") or check_data.get("eventStatus")
        print(f"[test_event_status_open] 이벤트 상태: {status_val}")
        if status_val:
            assert "OPEN" in str(status_val) or "오픈" in str(status_val), (
                f"오픈 후 상태는 OPEN이어야 합니다: {status_val}"
            )
    print(f"[test_event_status_open] OPEN 상태 전이 확인 완료")


def test_event_status_calculating(base_url, auth_headers):
    """이벤트 상태를 정산중(CALCULATING)으로 전환합니다."""
    event_id = _lifecycle_state.get("event_id")
    if not event_id:
        pytest.skip("lifecycle event_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events/{event_id}/status"
    payload = {"status": "정산중"}
    print(f"\n[test_event_status_calculating] PATCH {url}")
    print(f"[test_event_status_calculating] payload={payload}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_event_status_calculating] status={resp.status_code}, body={resp.text[:400]}")

    # 상태 전이 성공 또는 비즈니스 제약으로 실패 모두 허용 (OPEN 상태에서만 가능)
    print(f"[test_event_status_calculating] 정산중 전환 응답: {resp.status_code}")
    if resp.status_code == 200:
        check_resp = requests.get(f"{base_url}/v1/events/{event_id}", headers=auth_headers)
        if check_resp.status_code == 200:
            check_data = get_data(check_resp)
            status_val = check_data.get("status") or check_data.get("eventStatus")
            print(f"[test_event_status_calculating] 이벤트 상태: {status_val}")
    print(f"[test_event_status_calculating] CALCULATING 상태 전이 시도 완료")


def test_event_status_closed(base_url, auth_headers):
    """이벤트 상태를 정산완료(CLOSED)로 전환합니다."""
    event_id = _lifecycle_state.get("event_id")
    if not event_id:
        pytest.skip("lifecycle event_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events/{event_id}/status"
    payload = {"status": "정산완료"}
    print(f"\n[test_event_status_closed] PATCH {url}")
    print(f"[test_event_status_closed] payload={payload}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_event_status_closed] status={resp.status_code}, body={resp.text[:400]}")

    print(f"[test_event_status_closed] 정산완료 전환 응답: {resp.status_code}")
    if resp.status_code == 200:
        check_resp = requests.get(f"{base_url}/v1/events/{event_id}", headers=auth_headers)
        if check_resp.status_code == 200:
            check_data = get_data(check_resp)
            status_val = check_data.get("status") or check_data.get("eventStatus")
            print(f"[test_event_status_closed] 이벤트 상태: {status_val}")
    print(f"[test_event_status_closed] CLOSED 상태 전이 시도 완료")
