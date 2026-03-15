"""
이벤트 오픈 조건 개별 검증 E2E 시나리오 테스트.
각 테스트는 독립적인 신규 이벤트를 생성하여 오픈 조건을 하나씩 검증합니다.
"""
import pytest
import requests
from datetime import datetime, timedelta

from conftest import assert_status, get_data


def _future_date_str(days_ahead: int = 30) -> str:
    """현재로부터 days_ahead일 후의 날짜를 'yyyy.MM.dd HH:mm' 형식으로 반환합니다."""
    future = datetime.now() + timedelta(days=days_ahead)
    return future.strftime("%Y.%m.%d %H:%M")


def _create_fresh_event(base_url: str, auth_headers: dict, host_id: int, name: str) -> int:
    """테스트용 신규 이벤트를 생성하고 event_id를 반환합니다."""
    url = f"{base_url}/v1/events"
    payload = {
        "hostId": host_id,
        "name": name,
        "startAt": _future_date_str(60),
        "runTime": 90,
    }
    print(f"\n[_create_fresh_event] POST {url} name={name}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[_create_fresh_event] status={resp.status_code}, body={resp.text[:300]}")
    assert resp.status_code in (200, 201), f"이벤트 생성 실패: {resp.text[:300]}"
    return get_data(resp)["eventId"]


# 이 모듈 내에서만 사용하는 로컬 상태
_state: dict = {}


def test_open_without_basic(base_url, auth_headers, state):
    """기본정보 없이 오픈 시도하면 체크리스트 미충족으로 실패해야 합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    event_id = _create_fresh_event(base_url, auth_headers, state.host_id, "오픈조건테스트-기본정보없음")
    _state["no_basic_event_id"] = event_id

    # 기본정보/상세정보/티켓 없이 바로 오픈 시도
    open_url = f"{base_url}/v1/events/{event_id}/open"
    print(f"\n[test_open_without_basic] PATCH {open_url} (기본정보 없음)")
    resp = requests.patch(open_url, headers=auth_headers)
    print(f"[test_open_without_basic] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code != 200, (
        f"기본정보 없이 이벤트 오픈이 성공해서는 안 됩니다. status={resp.status_code}"
    )
    print(f"[test_open_without_basic] 기본정보 없는 오픈 차단 확인: {resp.status_code}")


def test_open_without_detail(base_url, auth_headers, state):
    """기본정보만 있고 상세정보 없이 오픈 시도하면 체크리스트 미충족으로 실패해야 합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    event_id = _create_fresh_event(base_url, auth_headers, state.host_id, "오픈조건테스트-상세정보없음")
    _state["no_detail_event_id"] = event_id

    # 기본정보 수정 (체크리스트 1항목 충족)
    basic_url = f"{base_url}/v1/events/{event_id}/basic"
    basic_payload = {
        "name": "오픈조건테스트-상세정보없음(수정)",
        "startAt": _future_date_str(60),
        "runTime": 120,
        "placeName": "테스트공연장",
        "placeAddress": "서울 마포구 어울마당로 35",
        "longitude": 126.920036,
        "latitude": 37.548369,
    }
    print(f"\n[test_open_without_detail] PATCH {basic_url}")
    basic_resp = requests.patch(basic_url, json=basic_payload, headers=auth_headers)
    print(f"[test_open_without_detail] basic status={basic_resp.status_code}, body={basic_resp.text[:300]}")
    assert_status(basic_resp, 200)

    # 상세정보/티켓 없이 오픈 시도
    open_url = f"{base_url}/v1/events/{event_id}/open"
    print(f"[test_open_without_detail] PATCH {open_url} (상세정보 없음)")
    resp = requests.patch(open_url, headers=auth_headers)
    print(f"[test_open_without_detail] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code != 200, (
        f"상세정보 없이 이벤트 오픈이 성공해서는 안 됩니다. status={resp.status_code}"
    )
    print(f"[test_open_without_detail] 상세정보 없는 오픈 차단 확인: {resp.status_code}")


def test_open_without_ticket(base_url, auth_headers, state):
    """기본정보+상세정보는 있지만 티켓 없이 오픈 시도하면 체크리스트 미충족으로 실패해야 합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    event_id = _create_fresh_event(base_url, auth_headers, state.host_id, "오픈조건테스트-티켓없음")
    _state["no_ticket_event_id"] = event_id

    # 기본정보 수정
    basic_url = f"{base_url}/v1/events/{event_id}/basic"
    basic_payload = {
        "name": "오픈조건테스트-티켓없음(수정)",
        "startAt": _future_date_str(60),
        "runTime": 120,
        "placeName": "테스트공연장",
        "placeAddress": "서울 마포구 어울마당로 35",
        "longitude": 126.920036,
        "latitude": 37.548369,
    }
    print(f"\n[test_open_without_ticket] PATCH {basic_url}")
    basic_resp = requests.patch(basic_url, json=basic_payload, headers=auth_headers)
    print(f"[test_open_without_ticket] basic status={basic_resp.status_code}, body={basic_resp.text[:300]}")
    assert_status(basic_resp, 200)

    # 상세정보 수정
    detail_url = f"{base_url}/v1/events/{event_id}/details"
    detail_payload = {
        "posterImageKey": "test/event/open-cond/poster.jpeg",
        "content": "오픈 조건 테스트 공연 (티켓 없음).",
    }
    print(f"[test_open_without_ticket] PATCH {detail_url}")
    detail_resp = requests.patch(detail_url, json=detail_payload, headers=auth_headers)
    print(f"[test_open_without_ticket] detail status={detail_resp.status_code}, body={detail_resp.text[:300]}")
    assert_status(detail_resp, 200)

    # 티켓 없이 오픈 시도
    open_url = f"{base_url}/v1/events/{event_id}/open"
    print(f"[test_open_without_ticket] PATCH {open_url} (티켓 없음)")
    resp = requests.patch(open_url, headers=auth_headers)
    print(f"[test_open_without_ticket] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code != 200, (
        f"티켓 없는 이벤트 오픈이 성공해서는 안 됩니다. status={resp.status_code}"
    )
    print(f"[test_open_without_ticket] 티켓 없는 오픈 차단 확인: {resp.status_code}")


def test_open_with_all_conditions(base_url, auth_headers, state):
    """기본정보+상세정보+티켓 모든 조건 충족 후 오픈 시도하면 성공해야 합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    event_id = _create_fresh_event(base_url, auth_headers, state.host_id, "오픈조건테스트-전체충족")
    _state["all_conditions_event_id"] = event_id

    # 기본정보 수정
    basic_url = f"{base_url}/v1/events/{event_id}/basic"
    basic_payload = {
        "name": "오픈조건테스트-전체충족(수정)",
        "startAt": _future_date_str(60),
        "runTime": 120,
        "placeName": "테스트공연장",
        "placeAddress": "서울 마포구 어울마당로 35",
        "longitude": 126.920036,
        "latitude": 37.548369,
    }
    print(f"\n[test_open_with_all_conditions] PATCH {basic_url}")
    basic_resp = requests.patch(basic_url, json=basic_payload, headers=auth_headers)
    print(f"[test_open_with_all_conditions] basic status={basic_resp.status_code}, body={basic_resp.text[:300]}")
    assert_status(basic_resp, 200)

    # 상세정보 수정
    detail_url = f"{base_url}/v1/events/{event_id}/details"
    detail_payload = {
        "posterImageKey": "test/event/open-cond/all-poster.jpeg",
        "content": "오픈 조건 전체 충족 테스트 공연입니다.",
    }
    print(f"[test_open_with_all_conditions] PATCH {detail_url}")
    detail_resp = requests.patch(detail_url, json=detail_payload, headers=auth_headers)
    print(f"[test_open_with_all_conditions] detail status={detail_resp.status_code}, body={detail_resp.text[:300]}")
    assert_status(detail_resp, 200)

    # 티켓 생성
    ticket_url = f"{base_url}/v1/events/{event_id}/ticketItems"
    ticket_payload = {
        "payType": "무료티켓",
        "name": "전체조건충족무료티켓",
        "description": "오픈 조건 전체 충족 테스트용 티켓",
        "price": 0,
        "supplyCount": 50,
        "approveType": "선착순",
        "isQuantityPublic": True,
        "purchaseLimit": 2,
    }
    print(f"[test_open_with_all_conditions] POST {ticket_url}")
    ticket_resp = requests.post(ticket_url, json=ticket_payload, headers=auth_headers)
    print(f"[test_open_with_all_conditions] ticket status={ticket_resp.status_code}, body={ticket_resp.text[:300]}")
    assert ticket_resp.status_code in (200, 201), f"티켓 생성 실패: {ticket_resp.text[:200]}"

    # 체크리스트 확인
    checklist_url = f"{base_url}/v1/events/{event_id}/checklist"
    print(f"[test_open_with_all_conditions] GET {checklist_url}")
    checklist_resp = requests.get(checklist_url, headers=auth_headers)
    print(f"[test_open_with_all_conditions] checklist status={checklist_resp.status_code}, body={checklist_resp.text[:400]}")

    # 모든 조건 충족 후 오픈 시도
    open_url = f"{base_url}/v1/events/{event_id}/open"
    print(f"[test_open_with_all_conditions] PATCH {open_url} (모든 조건 충족)")
    resp = requests.patch(open_url, headers=auth_headers)
    print(f"[test_open_with_all_conditions] open status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = get_data(resp)
    status_val = data.get("status") or data.get("eventStatus")
    print(f"[test_open_with_all_conditions] 오픈 후 이벤트 상태: {status_val}")
    if status_val:
        assert any(s in str(status_val) for s in ("OPEN", "오픈", "진행중")), (
            f"오픈 후 상태는 OPEN/진행중이어야 합니다: {status_val}"
        )
    _state["opened_event_id"] = event_id
    print(f"[test_open_with_all_conditions] 모든 조건 충족 오픈 성공: event_id={event_id}")
