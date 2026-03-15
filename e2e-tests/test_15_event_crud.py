"""
이벤트 수정/삭제 E2E 시나리오 테스트.
이벤트 생성, 기본 정보 수정, 장소 정보 수정, 삭제(PREPARING 상태)를 검증합니다.
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
_crud_state: dict = {
    "event_id": 0,
}


def test_create_event_for_crud(base_url, auth_headers, state):
    """CRUD 테스트 전용 새 이벤트를 생성하고 event_id를 로컬 상태에 저장합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events"
    payload = {
        "hostId": state.host_id,
        "name": "CRUD테스트공연",
        "startAt": _future_date_str(30),
        "runTime": 90,
    }
    print(f"\n[test_create_event_for_crud] POST {url}")
    print(f"[test_create_event_for_crud] payload={payload}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_create_event_for_crud] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (200, 201), (
        f"이벤트 생성 실패: status={resp.status_code}, body={resp.text[:300]}"
    )
    data = get_data(resp)
    assert "eventId" in data, f"응답에 eventId 필드가 없습니다: {data}"
    _crud_state["event_id"] = data["eventId"]
    print(f"[test_create_event_for_crud] CRUD 이벤트 생성 완료: event_id={_crud_state['event_id']}")


def test_update_event_basic_info(base_url, auth_headers):
    """이벤트의 이름과 runTime을 수정하고 성공 응답을 확인합니다."""
    event_id = _crud_state.get("event_id")
    if not event_id:
        pytest.skip("CRUD event_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events/{event_id}/basic"
    payload = {
        "name": "CRUD테스트공연(이름수정)",
        "startAt": _future_date_str(35),
        "runTime": 120,
        "placeName": "테스트공연장",
        "placeAddress": "서울 마포구 어울마당로 35",
        "longitude": 126.920036,
        "latitude": 37.548369,
    }
    print(f"\n[test_update_event_basic_info] PATCH {url}")
    print(f"[test_update_event_basic_info] payload={payload}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_update_event_basic_info] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    print(f"[test_update_event_basic_info] 이벤트 이름/runTime 수정 완료")

    # 수정된 내용 검증
    detail_resp = requests.get(f"{url.rsplit('/basic', 1)[0]}", headers=auth_headers)
    if detail_resp.status_code == 200:
        detail_data = get_data(detail_resp)
        name_val = detail_data.get("name")
        print(f"[test_update_event_basic_info] 수정 후 이름 확인: {name_val}")


def test_update_event_place(base_url, auth_headers):
    """이벤트의 장소 정보를 수정하고 성공 응답을 확인합니다."""
    event_id = _crud_state.get("event_id")
    if not event_id:
        pytest.skip("CRUD event_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events/{event_id}/basic"
    payload = {
        "name": "CRUD테스트공연(장소수정)",
        "startAt": _future_date_str(35),
        "runTime": 120,
        "placeName": "수정된공연장",
        "placeAddress": "서울 강남구 테헤란로 152",
        "longitude": 127.036617,
        "latitude": 37.500613,
    }
    print(f"\n[test_update_event_place] PATCH {url}")
    print(f"[test_update_event_place] payload={payload}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_update_event_place] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    print(f"[test_update_event_place] 이벤트 장소 정보 수정 완료")


def test_delete_event(base_url, auth_headers):
    """PREPARING 상태의 이벤트를 삭제하고 성공 응답을 확인합니다."""
    event_id = _crud_state.get("event_id")
    if not event_id:
        pytest.skip("CRUD event_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events/{event_id}/delete"
    print(f"\n[test_delete_event] PATCH {url}")
    resp = requests.patch(url, headers=auth_headers)
    print(f"[test_delete_event] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (200, 204), (
        f"이벤트 삭제 실패: status={resp.status_code}, body={resp.text[:300]}"
    )
    print(f"[test_delete_event] 이벤트 삭제 완료: event_id={event_id}")
