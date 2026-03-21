"""
어드민 역할 기반 접근 제어 E2E 테스트.
일반 USER 역할로 어드민 API에 접근 시 403이 반환되는지 검증합니다.

변경사항: 어드민 로컬 로그인 엔드포인트 삭제됨.
일반 auth 엔드포인트로 로그인한 USER 토큰으로 admin API 접근을 테스트합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


@pytest.fixture(scope="module")
def admin_base_url():
    """어드민 API 베이스 URL."""
    import os
    base = os.environ.get("API_BASE_URL", "http://localhost:8080/api")
    return base.replace("/api", "/internal-api")


@pytest.fixture(scope="module")
def normal_user_token():
    """
    일반 USER 역할로 로컬 로그인하여 토큰을 획득합니다.
    일반 auth 엔드포인트로 로그인하여 토큰을 획득합니다.
    """
    import os
    base = os.environ.get("API_BASE_URL", "http://localhost:8080/api")
    url = f"{base}/v1/auth/oauth/local/login"
    payload = {
        "email": "normaluser@dudoong.com",
        "name": "일반유저",
        "phoneNumber": "010-1234-5678",
        "profileImage": None,
        "marketingAgree": False,
    }
    print(f"\n[normal_user_token] POST {url}")
    resp = requests.post(url, json=payload)
    print(f"[normal_user_token] status={resp.status_code}, body={resp.text[:300]}")
    assert resp.status_code == 200, f"일반 유저 로그인 실패: {resp.text}"
    data = get_data(resp)
    return data["accessToken"]


@pytest.fixture(scope="module")
def normal_user_headers(normal_user_token):
    """일반 유저의 Authorization 헤더."""
    return {"Authorization": f"Bearer {normal_user_token}"}


def test_admin_dashboard_forbidden_for_user(admin_base_url, normal_user_headers):
    """일반 USER 토큰으로 어드민 대시보드 접근 시 403이 반환되는지 확인합니다."""
    url = f"{admin_base_url}/v1/dashboard"
    print(f"\n[test_admin_dashboard_forbidden] GET {url}")
    resp = requests.get(url, headers=normal_user_headers)
    print(f"[test_admin_dashboard_forbidden] status={resp.status_code}")

    assert resp.status_code == 403, (
        f"일반 USER가 어드민 대시보드에 접근할 수 있습니다. status={resp.status_code}"
    )
    print("[test_admin_dashboard_forbidden] 대시보드 접근 차단 확인")


def test_admin_users_forbidden_for_user(admin_base_url, normal_user_headers):
    """일반 USER 토큰으로 어드민 유저 목록 접근 시 403이 반환되는지 확인합니다."""
    url = f"{admin_base_url}/v1/users"
    print(f"\n[test_admin_users_forbidden] GET {url}")
    resp = requests.get(url, headers=normal_user_headers)
    print(f"[test_admin_users_forbidden] status={resp.status_code}")

    assert resp.status_code == 403, (
        f"일반 USER가 어드민 유저 목록에 접근할 수 있습니다. status={resp.status_code}"
    )
    print("[test_admin_users_forbidden] 유저 목록 접근 차단 확인")


def test_admin_events_forbidden_for_user(admin_base_url, normal_user_headers):
    """일반 USER 토큰으로 어드민 이벤트 목록 접근 시 403이 반환되는지 확인합니다."""
    url = f"{admin_base_url}/v1/events"
    print(f"\n[test_admin_events_forbidden] GET {url}")
    resp = requests.get(url, headers=normal_user_headers)
    print(f"[test_admin_events_forbidden] status={resp.status_code}")

    assert resp.status_code == 403, (
        f"일반 USER가 어드민 이벤트 목록에 접근할 수 있습니다. status={resp.status_code}"
    )
    print("[test_admin_events_forbidden] 이벤트 목록 접근 차단 확인")


def test_admin_unauthenticated_access(admin_base_url):
    """토큰 없이 어드민 대시보드에 접근하면 401 또는 403이 반환되는지 확인합니다."""
    url = f"{admin_base_url}/v1/dashboard"
    print(f"\n[test_admin_unauthenticated] GET {url} (토큰 없음)")
    resp = requests.get(url)
    print(f"[test_admin_unauthenticated] status={resp.status_code}")

    assert resp.status_code in (401, 403), (
        f"미인증 어드민 접근 시 401/403이 기대되지만 {resp.status_code}가 반환되었습니다"
    )
    print(f"[test_admin_unauthenticated] 미인증 접근 차단 확인: {resp.status_code}")
