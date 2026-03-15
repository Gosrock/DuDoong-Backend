"""
호스트 관련 E2E 시나리오 테스트.
호스트 생성 및 내가 속한 호스트 목록 조회를 검증합니다.
"""
import pytest
import requests

from conftest import assert_status


def test_create_host(base_url, auth_headers, state):
    """새로운 호스트를 생성하고 host_id를 state에 저장합니다."""
    url = f"{base_url}/v1/hosts"
    payload = {
        "name": "E2E테스트호스트",
        "contactEmail": "host@dudoong.com",
        "contactNumber": "010-1234-5678",
    }
    print(f"\n[test_create_host] POST {url}")
    print(f"[test_create_host] payload={payload}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_create_host] status={resp.status_code}, body={resp.text[:400]}")

    # 201 또는 200 모두 성공으로 허용
    assert resp.status_code in (200, 201), (
        f"호스트 생성 실패: status={resp.status_code}, body={resp.text[:300]}"
    )
    data = resp.json()
    assert "id" in data, f"응답에 id 필드가 없습니다: {data}"
    state.host_id = data["id"]
    print(f"[test_create_host] 호스트 생성 완료: host_id={state.host_id}")


def test_read_host_profiles(base_url, auth_headers, state):
    """내가 속한 호스트 목록을 조회하고 생성된 호스트가 포함되는지 확인합니다."""
    url = f"{base_url}/v1/hosts"
    print(f"\n[test_read_host_profiles] GET {url}")
    resp = requests.get(url, headers=auth_headers)
    print(f"[test_read_host_profiles] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = resp.json()
    # SliceResponse 구조: {"data": [...], "hasNext": bool}
    assert "data" in data, f"응답에 data 필드가 없습니다: {data}"
    print(f"[test_read_host_profiles] 호스트 목록 조회 완료: {len(data['data'])}개")
    if state.host_id:
        ids = [h.get("id") for h in data["data"]]
        assert state.host_id in ids, (
            f"생성된 host_id={state.host_id}가 목록에 없습니다: {ids}"
        )
        print(f"[test_read_host_profiles] 생성된 호스트 확인됨: id={state.host_id}")
