"""
유저 프로필 E2E 시나리오 테스트.
내 프로필 조회 및 마케팅 수신 동의 토글을 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


def test_read_my_profile(base_url, auth_headers):
    """내 프로필 정보를 조회하고 응답에 필수 필드가 포함되는지 확인합니다."""
    url = f"{base_url}/v1/users/me"
    print(f"\n[test_read_my_profile] GET {url}")
    resp = requests.get(url, headers=auth_headers)
    print(f"[test_read_my_profile] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = get_data(resp)
    # 유저 프로필에는 email 또는 name 필드가 포함되어야 합니다
    assert any(field in data for field in ("email", "name", "userId", "id")), (
        f"응답에 email/name/userId/id 중 하나도 없습니다: {data}"
    )
    print(f"[test_read_my_profile] 프로필 조회 완료: email={data.get('email')}, name={data.get('name')}")


def test_toggle_marketing(base_url, auth_headers):
    """마케팅 수신 동의 상태를 토글하고 성공 응답을 확인합니다."""
    url = f"{base_url}/v1/users/marketing"
    print(f"\n[test_toggle_marketing] PATCH {url}")
    resp = requests.patch(url, headers=auth_headers)
    print(f"[test_toggle_marketing] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = get_data(resp)
    print(f"[test_toggle_marketing] 마케팅 동의 토글 완료: {data}")

    # 다시 한번 토글하여 원래 상태로 복원
    print(f"[test_toggle_marketing] PATCH {url} (복원)")
    resp2 = requests.patch(url, headers=auth_headers)
    print(f"[test_toggle_marketing] 복원 status={resp2.status_code}, body={resp2.text[:400]}")
    assert_status(resp2, 200)
    print(f"[test_toggle_marketing] 마케팅 동의 원래 상태로 복원 완료")
