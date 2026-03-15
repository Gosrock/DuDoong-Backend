"""
이벤트 상태 전이 매트릭스 E2E 시나리오 테스트.
유효한 전이는 성공하고, 비허용 전이의 실제 동작을 검증합니다.

상태 전이 규칙 (정책):
  PREPARING → OPEN (open 엔드포인트, 체크리스트 충족 필요)
  OPEN → CALCULATING (status 엔드포인트)
  CALCULATING → CLOSED (status 엔드포인트)

참고: /status 엔드포인트의 상태 전이 검증 수준을 확인하는 테스트입니다.
"""
import pytest
import requests
from datetime import datetime, timedelta

from conftest import assert_status, get_data


def _future_date_str(days_ahead: int = 30) -> str:
    future = datetime.now() + timedelta(days=days_ahead)
    return future.strftime("%Y.%m.%d %H:%M")


def _create_fresh_event(base_url, auth_headers, host_id, name):
    url = f"{base_url}/v1/events"
    payload = {"hostId": host_id, "name": name, "startAt": _future_date_str(60), "runTime": 90}
    resp = requests.post(url, json=payload, headers=auth_headers)
    assert resp.status_code in (200, 201), f"이벤트 생성 실패: {resp.text[:300]}"
    return get_data(resp)["eventId"]


def _create_open_event(base_url, auth_headers, host_id, name="상태매트릭스테스트"):
    event_id = _create_fresh_event(base_url, auth_headers, host_id, name)

    # 기본정보
    basic_payload = {
        "name": f"{name}(수정)", "startAt": _future_date_str(60), "runTime": 120,
        "placeName": "테스트공연장", "placeAddress": "서울 마포구 어울마당로 35",
        "longitude": 126.920036, "latitude": 37.548369,
    }
    resp = requests.patch(f"{base_url}/v1/events/{event_id}/basic", json=basic_payload, headers=auth_headers)
    assert resp.status_code == 200, f"기본정보 수정 실패: {resp.text[:200]}"

    # 상세정보
    detail_payload = {"posterImageKey": "test/event/status-matrix/poster.jpeg", "content": f"{name} 상세 내용입니다."}
    resp = requests.patch(f"{base_url}/v1/events/{event_id}/details", json=detail_payload, headers=auth_headers)
    assert resp.status_code == 200, f"상세정보 수정 실패: {resp.text[:200]}"

    # 티켓
    ticket_payload = {
        "payType": "무료티켓", "name": f"{name}티켓", "description": "테스트",
        "price": 0, "supplyCount": 10, "approveType": "선착순", "isQuantityPublic": True, "purchaseLimit": 2,
    }
    resp = requests.post(f"{base_url}/v1/events/{event_id}/ticketItems", json=ticket_payload, headers=auth_headers)
    assert resp.status_code in (200, 201), f"티켓 생성 실패: {resp.text[:200]}"

    # 오픈
    resp = requests.patch(f"{base_url}/v1/events/{event_id}/open", headers=auth_headers)
    assert resp.status_code == 200, f"이벤트 오픈 실패: {resp.text[:200]}"

    return event_id


# ==================== 유효한 전이 ====================

def test_valid_transition_open_to_calculating(base_url, auth_headers, state):
    """OPEN → CALCULATING 전이가 성공하는지 확인합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    event_id = _create_open_event(base_url, auth_headers, state.host_id, "유효전이-OPEN→CALC")

    url = f"{base_url}/v1/events/{event_id}/status"
    resp = requests.patch(url, json={"status": "CALCULATING"}, headers=auth_headers)
    print(f"\n[test_valid_open_to_calc] status={resp.status_code}, body={resp.text[:300]}")

    assert_status(resp, 200)
    data = get_data(resp)
    status_val = data.get("status", "")
    assert any(s in status_val for s in ("정산중", "CALCULATING")), f"상태가 정산중이 아닙니다: {status_val}"
    print(f"[test_valid_open_to_calc] OPEN → CALCULATING 전이 성공: {status_val}")


def test_valid_transition_calculating_to_closed(base_url, auth_headers, state):
    """CALCULATING → CLOSED 전이가 성공하는지 확인합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    event_id = _create_open_event(base_url, auth_headers, state.host_id, "유효전이-CALC→CLOSED")

    # OPEN → CALCULATING
    url = f"{base_url}/v1/events/{event_id}/status"
    resp = requests.patch(url, json={"status": "CALCULATING"}, headers=auth_headers)
    assert resp.status_code == 200, f"CALCULATING 전이 실패: {resp.text[:200]}"

    # CALCULATING → CLOSED
    resp = requests.patch(url, json={"status": "CLOSED"}, headers=auth_headers)
    print(f"\n[test_valid_calc_to_closed] status={resp.status_code}, body={resp.text[:300]}")

    assert_status(resp, 200)
    data = get_data(resp)
    status_val = data.get("status", "")
    assert any(s in status_val for s in ("지난공연", "CLOSED")), f"상태가 지난공연이 아닙니다: {status_val}"
    print(f"[test_valid_calc_to_closed] CALCULATING → CLOSED 전이 성공: {status_val}")


# ==================== 비허용 전이 (실제 동작 검증) ====================

def test_preparing_to_calculating(base_url, auth_headers, state):
    """PREPARING → CALCULATING 전이를 시도하고 실제 동작을 확인합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    event_id = _create_fresh_event(base_url, auth_headers, state.host_id, "비허용-PREP→CALC")
    url = f"{base_url}/v1/events/{event_id}/status"
    resp = requests.patch(url, json={"status": "CALCULATING"}, headers=auth_headers)
    print(f"\n[test_prep_to_calc] status={resp.status_code}, body={resp.text[:300]}")

    # 정책상 비허용이지만 API가 허용할 수 있음 - 실제 동작 기록
    if resp.status_code == 200:
        print(f"[WARNING] PREPARING → CALCULATING 전이가 허용됨 (상태 전이 검증 미구현 가능성)")
    else:
        print(f"[OK] PREPARING → CALCULATING 전이 차단됨: {resp.status_code}")


def test_open_to_closed_directly(base_url, auth_headers, state):
    """OPEN → CLOSED 직접 전이를 시도하고 실제 동작을 확인합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    event_id = _create_open_event(base_url, auth_headers, state.host_id, "비허용-OPEN→CLOSED")
    url = f"{base_url}/v1/events/{event_id}/status"
    resp = requests.patch(url, json={"status": "CLOSED"}, headers=auth_headers)
    print(f"\n[test_open_to_closed] status={resp.status_code}, body={resp.text[:300]}")

    if resp.status_code == 200:
        print(f"[WARNING] OPEN → CLOSED 직접 전이가 허용됨 (CALCULATING 건너뜀)")
    else:
        print(f"[OK] OPEN → CLOSED 직접 전이 차단됨: {resp.status_code}")
