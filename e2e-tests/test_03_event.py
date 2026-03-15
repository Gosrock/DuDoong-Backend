"""
이벤트(공연) 관련 E2E 시나리오 테스트.
이벤트 생성, 기본 정보 수정, 상세 정보 수정, 상세 조회, 검색을 검증합니다.
"""
import pytest
import requests
from datetime import datetime, timedelta

from conftest import assert_status, get_data, get_data


def _future_date_str(days_ahead: int = 30) -> str:
    """현재로부터 days_ahead일 후의 날짜를 'yyyy.MM.dd HH:mm' 형식으로 반환합니다."""
    future = datetime.now() + timedelta(days=days_ahead)
    return future.strftime("%Y.%m.%d %H:%M")


def test_create_event(base_url, auth_headers, state):
    """새로운 이벤트(공연)를 생성하고 event_id를 state에 저장합니다."""
    assert state.host_id, "host_id가 없습니다. test_02_host를 먼저 실행하세요."
    url = f"{base_url}/v1/events"
    payload = {
        "hostId": state.host_id,
        "name": "E2E테스트공연",
        "startAt": _future_date_str(30),
        "runTime": 90,
    }
    print(f"\n[test_create_event] POST {url}")
    print(f"[test_create_event] payload={payload}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_create_event] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (200, 201), (
        f"이벤트 생성 실패: status={resp.status_code}, body={resp.text[:300]}"
    )
    data = get_data(resp)
    assert "eventId" in data, f"응답에 eventId 필드가 없습니다: {data}"
    state.event_id = data["eventId"]
    print(f"[test_create_event] 이벤트 생성 완료: event_id={state.event_id}")


def test_update_event_basic(base_url, auth_headers, state):
    """이벤트 기본 정보(이름, 시작시각, 장소 등)를 수정합니다."""
    assert state.event_id, "event_id가 없습니다. test_create_event를 먼저 실행하세요."
    url = f"{base_url}/v1/events/{state.event_id}/basic"
    payload = {
        "name": "E2E테스트공연(수정됨)",
        "startAt": _future_date_str(45),
        "runTime": 120,
        "placeName": "테스트공연장",
        "placeAddress": "서울 마포구 어울마당로 35",
        "longitude": 126.920036,
        "latitude": 37.548369,
    }
    print(f"\n[test_update_event_basic] PATCH {url}")
    print(f"[test_update_event_basic] payload={payload}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_update_event_basic] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    print(f"[test_update_event_basic] 기본 정보 수정 완료")


def test_update_event_detail(base_url, auth_headers, state):
    """이벤트 상세 정보(포스터 이미지, 공연 내용)를 수정합니다."""
    assert state.event_id, "event_id가 없습니다."
    url = f"{base_url}/v1/events/{state.event_id}/details"
    payload = {
        "posterImageKey": "test/event/e2e/poster.jpeg",
        "content": "E2E 테스트 공연에 오신 것을 환영합니다.",
    }
    print(f"\n[test_update_event_detail] PATCH {url}")
    print(f"[test_update_event_detail] payload={payload}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_update_event_detail] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    print(f"[test_update_event_detail] 상세 정보 수정 완료")


def test_search_events(base_url):
    """이벤트 이름 키워드 검색이 동작하는지 확인합니다 (인증 불필요)."""
    url = f"{base_url}/v1/events/search"
    params = {"keyword": "E2E"}
    print(f"\n[test_search_events] GET {url} params={params}")
    resp = requests.get(url, params=params)
    print(f"[test_search_events] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = get_data(resp)
    assert "content" in data, f"응답에 content 필드가 없습니다: {data}"
    print(f"[test_search_events] 검색 결과: {len(data['content'])}개")
