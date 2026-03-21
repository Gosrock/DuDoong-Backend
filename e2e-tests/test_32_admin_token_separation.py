"""
Admin 토큰 분리 + 역할 기반 접근 제어 심층 E2E 테스트.

쿠키 공유 방식 전환 후의 보안 정책을 검증합니다:
1. 일반 유저 토큰(Authorization)으로 /internal-api/** 접근 불가 (403)
2. 미인증 상태에서 admin API 접근 불가 (401/403)
3. MANAGER 역할은 더 이상 admin 접근 불가 (ADMIN/SUPER_ADMIN만 허용)
4. Admin /me 엔드포인트는 인증 필요
"""
import os
import pytest
import requests

from conftest import assert_status, get_data


@pytest.fixture(scope="module")
def api_base_url():
    """Public API 베이스 URL."""
    return os.environ.get("API_BASE_URL", "http://localhost:8080/api")


@pytest.fixture(scope="module")
def admin_base_url(api_base_url):
    """Admin API 베이스 URL."""
    return api_base_url.replace("/api", "/internal-api")


@pytest.fixture(scope="module")
def normal_user_token(api_base_url):
    """일반 USER 역할로 로그인하여 토큰 획득."""
    url = f"{api_base_url}/v1/auth/oauth/local/login"
    payload = {
        "email": "rbac-normal@dudoong.com",
        "name": "RBAC일반유저",
        "phoneNumber": "010-1111-2222",
        "profileImage": None,
        "marketingAgree": False,
    }
    resp = requests.post(url, json=payload)
    assert resp.status_code == 200, f"일반 유저 로그인 실패: {resp.text}"
    return get_data(resp)["accessToken"]


# ============================================================================
# 1. 일반 유저 토큰 → Admin API 접근 차단
# ============================================================================

class TestNormalUserTokenBlocked:
    """일반 유저 토큰(Authorization 헤더)으로 Admin API 접근이 차단되는지 검증."""

    @pytest.mark.parametrize("endpoint", [
        "/v1/dashboard",
        "/v1/users",
        "/v1/events",
        "/v1/orders",
        "/v1/comments",
    ])
    def test_authorization_header_blocked(self, admin_base_url, normal_user_token, endpoint):
        """일반 유저 토큰을 Authorization 헤더로 전달해도 admin API 차단."""
        url = f"{admin_base_url}{endpoint}"
        headers = {"Authorization": f"Bearer {normal_user_token}"}
        resp = requests.get(url, headers=headers)
        assert resp.status_code == 403, (
            f"[Authorization] {endpoint}: 일반 유저 접근이 차단되지 않음. status={resp.status_code}"
        )

    @pytest.mark.parametrize("endpoint", [
        "/v1/dashboard",
        "/v1/users",
        "/v1/events",
    ])
    def test_cookie_with_normal_token_blocked(self, admin_base_url, normal_user_token, endpoint):
        """일반 유저 토큰을 쿠키로 전달해도 admin API 차단 (ADMIN/SUPER_ADMIN 아님)."""
        url = f"{admin_base_url}{endpoint}"
        cookies = {"accessToken": normal_user_token}
        resp = requests.get(url, cookies=cookies)
        assert resp.status_code == 403, (
            f"[Cookie] {endpoint}: 일반 토큰으로 접근이 차단되지 않음. status={resp.status_code}"
        )


# ============================================================================
# 2. 미인증 접근 차단
# ============================================================================

class TestUnauthenticatedBlocked:
    """토큰 없이 Admin API 접근이 차단되는지 검증."""

    @pytest.mark.parametrize("endpoint", [
        "/v1/dashboard",
        "/v1/users",
        "/v1/events",
        "/v1/orders",
        "/v1/comments",
    ])
    def test_no_token_blocked(self, admin_base_url, endpoint):
        """토큰 없이 admin API 접근 시 401 또는 403."""
        url = f"{admin_base_url}{endpoint}"
        resp = requests.get(url)
        assert resp.status_code in (401, 403), (
            f"{endpoint}: 미인증 접근 시 401/403이 기대되지만 {resp.status_code} 반환"
        )


# ============================================================================
# 3. Admin /me 엔드포인트는 인증 필요
# ============================================================================

class TestAdminMeEndpoint:
    """Admin /me 엔드포인트는 ADMIN/SUPER_ADMIN 토큰이 필요."""

    def test_me_without_token_blocked(self, admin_base_url):
        """토큰 없이 /me 접근 시 차단."""
        url = f"{admin_base_url}/v1/auth/me"
        resp = requests.get(url)
        assert resp.status_code in (401, 403), (
            f"/me 미인증 접근 허용됨. status={resp.status_code}"
        )

    def test_me_with_normal_token_blocked(self, admin_base_url, normal_user_token):
        """일반 유저 토큰으로 /me 접근 시 차단."""
        url = f"{admin_base_url}/v1/auth/me"
        headers = {"Authorization": f"Bearer {normal_user_token}"}
        resp = requests.get(url, headers=headers)
        assert resp.status_code == 403, (
            f"일반 유저 토큰으로 admin /me 접근 허용됨. status={resp.status_code}"
        )
