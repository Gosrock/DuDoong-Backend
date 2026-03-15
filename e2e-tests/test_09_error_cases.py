"""
에러/예외 케이스 E2E 시나리오 테스트.
유효성 검증 오류, 존재하지 않는 리소스, 인증 오류, 비즈니스 규칙 위반을 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


def test_create_host_empty_name(base_url, auth_headers):
    """빈 이름으로 호스트를 생성하면 400 Bad Request가 반환되는지 확인합니다."""
    url = f"{base_url}/v1/hosts"
    payload = {
        "name": "",
        "contactEmail": "host@dudoong.com",
        "contactNumber": "010-1234-5678",
    }
    print(f"\n[test_create_host_empty_name] POST {url}")
    print(f"[test_create_host_empty_name] payload={payload}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_create_host_empty_name] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code == 400, (
        f"빈 이름 호스트 생성 시 400이 기대되지만 {resp.status_code}가 반환되었습니다"
    )
    print(f"[test_create_host_empty_name] 빈 이름 유효성 검증 확인: {resp.status_code}")


def test_create_event_missing_fields(base_url, auth_headers):
    """hostId 없이 이벤트를 생성하면 400 Bad Request가 반환되는지 확인합니다."""
    url = f"{base_url}/v1/events"
    payload = {
        "name": "에러테스트공연",
        # hostId 누락
    }
    print(f"\n[test_create_event_missing_fields] POST {url}")
    print(f"[test_create_event_missing_fields] payload={payload}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_create_event_missing_fields] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code == 400, (
        f"필수 필드 누락 시 400이 기대되지만 {resp.status_code}가 반환되었습니다"
    )
    print(f"[test_create_event_missing_fields] 필수 필드 누락 유효성 검증 확인: {resp.status_code}")


def test_create_ticket_invalid_price(base_url, auth_headers, state):
    """음수 가격으로 티켓을 생성하면 400 Bad Request가 반환되는지 확인합니다."""
    if not state.event_id:
        pytest.skip("event_id가 없어 테스트를 건너뜁니다.")
    url = f"{base_url}/v1/events/{state.event_id}/ticketItems"
    payload = {
        "payType": "유료티켓",
        "name": "음수가격티켓",
        "description": "잘못된 가격 테스트",
        "price": -1000,
        "supplyCount": 10,
        "approveType": "선착순",
        "isQuantityPublic": True,
        "purchaseLimit": 1,
    }
    print(f"\n[test_create_ticket_invalid_price] POST {url}")
    print(f"[test_create_ticket_invalid_price] payload={payload}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_create_ticket_invalid_price] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code == 400, (
        f"음수 가격 티켓 생성 시 400이 기대되지만 {resp.status_code}가 반환되었습니다"
    )
    print(f"[test_create_ticket_invalid_price] 음수 가격 유효성 검증 확인: {resp.status_code}")


def test_read_nonexistent_event(base_url):
    """존재하지 않는 이벤트 ID로 조회하면 404 Not Found가 반환되는지 확인합니다."""
    url = f"{base_url}/v1/events/99999999"
    print(f"\n[test_read_nonexistent_event] GET {url}")
    resp = requests.get(url)
    print(f"[test_read_nonexistent_event] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code == 404, (
        f"존재하지 않는 이벤트 조회 시 404가 기대되지만 {resp.status_code}가 반환되었습니다"
    )
    print(f"[test_read_nonexistent_event] 404 Not Found 확인")


def test_expired_token(base_url):
    """쓰레기 토큰으로 인증이 필요한 엔드포인트에 접근하면 401 또는 403이 반환되는지 확인합니다."""
    url = f"{base_url}/v1/hosts"
    garbage_headers = {"Authorization": "Bearer this.is.garbage.token"}
    print(f"\n[test_expired_token] GET {url} (쓰레기 토큰)")
    resp = requests.get(url, headers=garbage_headers)
    print(f"[test_expired_token] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (401, 403), (
        f"유효하지 않은 토큰 사용 시 401 또는 403이 기대되지만 {resp.status_code}가 반환되었습니다"
    )
    print(f"[test_expired_token] 유효하지 않은 토큰 차단 확인: {resp.status_code}")


def test_open_event_without_ticket(base_url, auth_headers, state):
    """
    티켓 상품이 없는 이벤트를 오픈하려 하면 실패해야 합니다.
    새 이벤트를 생성하고 기본/상세 정보 없이 바로 오픈 시도합니다.
    """
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    from datetime import datetime, timedelta
    future = (datetime.now() + timedelta(days=60)).strftime("%Y.%m.%d %H:%M")

    # 새 이벤트 생성 (티켓 없음)
    create_url = f"{base_url}/v1/events"
    create_payload = {
        "hostId": state.host_id,
        "name": "티켓없는테스트공연",
        "startAt": future,
        "runTime": 60,
    }
    print(f"\n[test_open_event_without_ticket] POST {create_url}")
    create_resp = requests.post(create_url, json=create_payload, headers=auth_headers)
    print(f"[test_open_event_without_ticket] create status={create_resp.status_code}, body={create_resp.text[:300]}")

    if create_resp.status_code not in (200, 201):
        pytest.skip(f"이벤트 생성 실패로 테스트를 건너뜁니다: {create_resp.text[:200]}")

    new_event_id = get_data(create_resp)["eventId"]
    print(f"[test_open_event_without_ticket] 새 이벤트 생성됨: event_id={new_event_id}")

    # 티켓 없이 바로 오픈 시도
    open_url = f"{base_url}/v1/events/{new_event_id}/open"
    print(f"[test_open_event_without_ticket] PATCH {open_url} (티켓 없음)")
    open_resp = requests.patch(open_url, headers=auth_headers)
    print(f"[test_open_event_without_ticket] open status={open_resp.status_code}, body={open_resp.text[:400]}")

    assert open_resp.status_code != 200, (
        f"티켓 없는 이벤트 오픈이 성공해서는 안 됩니다. status={open_resp.status_code}"
    )
    print(f"[test_open_event_without_ticket] 티켓 없는 오픈 차단 확인: {open_resp.status_code}")
