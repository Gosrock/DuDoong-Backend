"""
호스트 관리 E2E 시나리오 테스트.
호스트 프로필 수정 및 호스트 상세 조회를 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


def test_update_host_profile(base_url, auth_headers, state):
    """호스트 프로필(이름, 소개)을 수정하고 성공 응답을 확인합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/hosts/{state.host_id}"
    payload = {
        "name": "E2E테스트호스트(수정됨)",
        "contactEmail": "updated-host@dudoong.com",
        "contactNumber": "010-9999-8888",
        "introduce": "E2E 테스트를 위한 호스트입니다.",
    }
    print(f"\n[test_update_host_profile] PATCH {url}")
    print(f"[test_update_host_profile] payload={payload}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_update_host_profile] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    print(f"[test_update_host_profile] 호스트 프로필 수정 완료")


def test_read_host_detail(base_url, auth_headers, state):
    """호스트 상세 정보를 조회하고 응답에 필수 필드가 포함되는지 확인합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/hosts/{state.host_id}"
    print(f"\n[test_read_host_detail] GET {url}")
    resp = requests.get(url, headers=auth_headers)
    print(f"[test_read_host_detail] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = get_data(resp)
    assert "hostId" in data or "id" in data, f"응답에 hostId 또는 id 필드가 없습니다: {data}"
    host_id_val = data.get("hostId") or data.get("id")
    assert host_id_val == state.host_id, (
        f"반환된 hostId={host_id_val}가 기대값 {state.host_id}와 다릅니다"
    )
    print(f"[test_read_host_detail] 호스트 상세 조회 완료: hostId={host_id_val}, name={data.get('name')}")
