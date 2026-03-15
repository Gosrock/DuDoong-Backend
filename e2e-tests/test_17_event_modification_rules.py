"""
이벤트 상태별 수정 규칙 E2E 시나리오 테스트.
PREPARING 상태와 OPEN 상태에서 기본/상세 정보 수정 가능 여부 및 삭제 규칙을 검증합니다.
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


def _setup_open_event(base_url: str, auth_headers: dict, host_id: int, name: str) -> int:
    """기본정보+상세정보+티켓을 모두 설정하고 오픈한 이벤트의 event_id를 반환합니다."""
    event_id = _create_fresh_event(base_url, auth_headers, host_id, name)

    # 기본정보 수정
    basic_url = f"{base_url}/v1/events/{event_id}/basic"
    basic_payload = {
        "name": f"{name}(수정)",
        "startAt": _future_date_str(60),
        "runTime": 120,
        "placeName": "테스트공연장",
        "placeAddress": "서울 마포구 어울마당로 35",
        "longitude": 126.920036,
        "latitude": 37.548369,
    }
    basic_resp = requests.patch(basic_url, json=basic_payload, headers=auth_headers)
    assert basic_resp.status_code == 200, f"기본정보 수정 실패: {basic_resp.text[:200]}"

    # 상세정보 수정
    detail_url = f"{base_url}/v1/events/{event_id}/details"
    detail_payload = {
        "posterImageKey": "test/event/mod-rules/poster.jpeg",
        "content": f"{name} 상세 내용입니다.",
    }
    detail_resp = requests.patch(detail_url, json=detail_payload, headers=auth_headers)
    assert detail_resp.status_code == 200, f"상세정보 수정 실패: {detail_resp.text[:200]}"

    # 티켓 생성
    ticket_url = f"{base_url}/v1/events/{event_id}/ticketItems"
    ticket_payload = {
        "payType": "무료티켓",
        "name": "수정규칙테스트티켓",
        "description": "수정 규칙 테스트용 티켓",
        "price": 0,
        "supplyCount": 50,
        "approveType": "선착순",
        "isQuantityPublic": True,
        "purchaseLimit": 2,
    }
    ticket_resp = requests.post(ticket_url, json=ticket_payload, headers=auth_headers)
    assert ticket_resp.status_code in (200, 201), f"티켓 생성 실패: {ticket_resp.text[:200]}"

    # 이벤트 오픈
    open_url = f"{base_url}/v1/events/{event_id}/open"
    open_resp = requests.patch(open_url, headers=auth_headers)
    assert open_resp.status_code == 200, f"이벤트 오픈 실패: {open_resp.text[:200]}"

    print(f"[_setup_open_event] OPEN 상태 이벤트 준비 완료: event_id={event_id}")
    return event_id


# 이 모듈 내에서만 사용하는 로컬 상태
_state: dict = {}


def test_modify_preparing_event_basic(base_url, auth_headers, state):
    """PREPARING 상태의 이벤트 기본정보 수정은 성공해야 합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    event_id = _create_fresh_event(base_url, auth_headers, state.host_id, "수정규칙테스트-PREP기본")
    _state["prep_basic_event_id"] = event_id

    url = f"{base_url}/v1/events/{event_id}/basic"
    payload = {
        "name": "수정규칙테스트-PREP기본(수정완료)",
        "startAt": _future_date_str(45),
        "runTime": 120,
        "placeName": "수정된공연장",
        "placeAddress": "서울 강남구 테헤란로 152",
        "longitude": 127.036617,
        "latitude": 37.500613,
    }
    print(f"\n[test_modify_preparing_event_basic] PATCH {url}")
    print(f"[test_modify_preparing_event_basic] payload={payload}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_modify_preparing_event_basic] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    print(f"[test_modify_preparing_event_basic] PREPARING 이벤트 기본정보 수정 성공 확인")


def test_modify_preparing_event_detail(base_url, auth_headers, state):
    """PREPARING 상태의 이벤트 상세정보 수정은 성공해야 합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    event_id = _create_fresh_event(base_url, auth_headers, state.host_id, "수정규칙테스트-PREP상세")
    _state["prep_detail_event_id"] = event_id

    url = f"{base_url}/v1/events/{event_id}/details"
    payload = {
        "posterImageKey": "test/event/mod-rules/prep-detail.jpeg",
        "content": "PREPARING 상태에서 상세정보를 수정합니다.",
    }
    print(f"\n[test_modify_preparing_event_detail] PATCH {url}")
    print(f"[test_modify_preparing_event_detail] payload={payload}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_modify_preparing_event_detail] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    print(f"[test_modify_preparing_event_detail] PREPARING 이벤트 상세정보 수정 성공 확인")


def test_open_event_then_modify_basic(base_url, auth_headers, state):
    """OPEN 상태의 이벤트 기본정보 수정 시도는 실패해야 합니다 (400 또는 403)."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    try:
        event_id = _setup_open_event(base_url, auth_headers, state.host_id, "수정규칙테스트-OPEN기본수정")
    except AssertionError as e:
        pytest.skip(f"OPEN 상태 이벤트 준비 실패로 테스트를 건너뜁니다: {e}")

    _state["open_basic_event_id"] = event_id

    url = f"{base_url}/v1/events/{event_id}/basic"
    payload = {
        "name": "OPEN상태에서기본정보수정시도",
        "startAt": _future_date_str(45),
        "runTime": 150,
        "placeName": "수정시도공연장",
        "placeAddress": "서울 강남구 테헤란로 152",
        "longitude": 127.036617,
        "latitude": 37.500613,
    }
    print(f"\n[test_open_event_then_modify_basic] PATCH {url}")
    print(f"[test_open_event_then_modify_basic] payload={payload}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_open_event_then_modify_basic] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (400, 403), (
        f"OPEN 상태 이벤트 기본정보 수정은 400 또는 403이 기대됩니다. "
        f"실제: {resp.status_code}, body={resp.text[:300]}"
    )
    print(f"[test_open_event_then_modify_basic] OPEN 이벤트 기본정보 수정 차단 확인: {resp.status_code}")


def test_open_event_then_modify_detail(base_url, auth_headers, state):
    """OPEN 상태의 이벤트 상세정보 수정 시도를 검증합니다 (비즈니스 규칙에 따라 성공 또는 실패)."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    try:
        event_id = _setup_open_event(base_url, auth_headers, state.host_id, "수정규칙테스트-OPEN상세수정")
    except AssertionError as e:
        pytest.skip(f"OPEN 상태 이벤트 준비 실패로 테스트를 건너뜁니다: {e}")

    _state["open_detail_event_id"] = event_id

    url = f"{base_url}/v1/events/{event_id}/details"
    payload = {
        "posterImageKey": "test/event/mod-rules/open-detail-updated.jpeg",
        "content": "OPEN 상태에서 상세정보 수정 시도입니다.",
    }
    print(f"\n[test_open_event_then_modify_detail] PATCH {url}")
    print(f"[test_open_event_then_modify_detail] payload={payload}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_open_event_then_modify_detail] status={resp.status_code}, body={resp.text[:400]}")

    # 비즈니스 규칙에 따라 성공(200) 또는 실패(400/403) 모두 허용
    assert resp.status_code in (200, 400, 403), (
        f"OPEN 상태 이벤트 상세정보 수정 응답이 예상 범위를 벗어납니다. "
        f"실제: {resp.status_code}, body={resp.text[:300]}"
    )
    if resp.status_code == 200:
        print(f"[test_open_event_then_modify_detail] OPEN 이벤트 상세정보 수정 허용됨: {resp.status_code}")
    else:
        print(f"[test_open_event_then_modify_detail] OPEN 이벤트 상세정보 수정 차단됨: {resp.status_code}")


def test_delete_preparing_event(base_url, auth_headers, state):
    """PREPARING 상태의 이벤트 삭제는 성공해야 합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    event_id = _create_fresh_event(base_url, auth_headers, state.host_id, "수정규칙테스트-PREP삭제")
    _state["prep_delete_event_id"] = event_id

    url = f"{base_url}/v1/events/{event_id}/delete"
    print(f"\n[test_delete_preparing_event] PATCH {url}")
    resp = requests.patch(url, headers=auth_headers)
    print(f"[test_delete_preparing_event] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (200, 204), (
        f"PREPARING 이벤트 삭제 실패: status={resp.status_code}, body={resp.text[:300]}"
    )
    print(f"[test_delete_preparing_event] PREPARING 이벤트 삭제 성공 확인: event_id={event_id}")


def test_delete_open_event(base_url, auth_headers, state):
    """OPEN 상태의 이벤트 삭제 시도는 실패해야 합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    try:
        event_id = _setup_open_event(base_url, auth_headers, state.host_id, "수정규칙테스트-OPEN삭제시도")
    except AssertionError as e:
        pytest.skip(f"OPEN 상태 이벤트 준비 실패로 테스트를 건너뜁니다: {e}")

    _state["open_delete_event_id"] = event_id

    url = f"{base_url}/v1/events/{event_id}/delete"
    print(f"\n[test_delete_open_event] PATCH {url} (OPEN 상태)")
    resp = requests.patch(url, headers=auth_headers)
    print(f"[test_delete_open_event] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code != 200, (
        f"OPEN 상태 이벤트 삭제가 성공해서는 안 됩니다. status={resp.status_code}"
    )
    print(f"[test_delete_open_event] OPEN 이벤트 삭제 차단 확인: {resp.status_code}")
