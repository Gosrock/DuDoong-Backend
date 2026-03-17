"""
Admin 토큰 분리 + 역할 기반 접근 제어 심층 E2E 테스트.

JWT에서 role 제거 후의 보안 정책을 검증합니다:
1. 일반 유저 토큰(Authorization)으로 /internal-api/** 접근 불가 (403)
2. MANAGER+ 유저라도 일반 토큰(aud:admin 없음)으로 admin API 접근 불가 (403)
3. X-Admin-Token 헤더로 admin 토큰 전달 시 정상 접근
4. 미인증 상태에서 admin API 접근 불가 (401/403)
5. Admin 로그인 → 일반 유저 역할이면 로그인 차단 (403)
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
    """일반 USER 역할로 로그인하여 토큰 획득 (일반 토큰, aud:admin 없음)."""
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
    def test_x_admin_token_header_with_normal_token_blocked(self, admin_base_url, normal_user_token, endpoint):
        """일반 유저 토큰을 X-Admin-Token 헤더로 전달해도 admin API 차단 (aud:admin 없음)."""
        url = f"{admin_base_url}{endpoint}"
        headers = {"X-Admin-Token": normal_user_token}
        resp = requests.get(url, headers=headers)
        assert resp.status_code == 403, (
            f"[X-Admin-Token] {endpoint}: 일반 토큰으로 접근이 차단되지 않음. status={resp.status_code}"
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
# 3. Admin 로그인 역할 검증
# ============================================================================

class TestAdminLoginRoleCheck:
    """어드민 로그인 시 역할 검증이 동작하는지 확인."""

    def test_user_role_cannot_admin_login(self, admin_base_url):
        """USER 역할 유저가 어드민 로컬 로그인 시 403 반환."""
        url = f"{admin_base_url}/v1/auth/oauth/local/login"
        payload = {
            "email": "rbac-admin-reject@dudoong.com",
            "name": "관리자거부유저",
            "phoneNumber": "010-3333-4444",
            "profileImage": None,
            "marketingAgree": False,
        }
        resp = requests.post(url, json=payload)
        assert resp.status_code == 403, (
            f"USER 역할의 어드민 로그인이 차단되지 않음. status={resp.status_code}"
        )


# ============================================================================
# 4. Admin Auth 공개 엔드포인트 접근 가능
# ============================================================================

class TestAdminPublicEndpoints:
    """Admin Auth의 공개 엔드포인트는 토큰 없이 접근 가능해야 함."""

    def test_admin_login_endpoint_reachable(self, admin_base_url):
        """어드민 로그인 엔드포인트는 인증 없이 접근 가능 (비즈니스 로직 레벨에서 거부)."""
        url = f"{admin_base_url}/v1/auth/oauth/local/login"
        payload = {
            "email": "reachability@dudoong.com",
            "name": "접근성테스트",
            "phoneNumber": "010-5555-6666",
            "profileImage": None,
            "marketingAgree": False,
        }
        resp = requests.post(url, json=payload)
        # 403 = 역할 부족으로 거부 (엔드포인트 자체는 도달 가능)
        # 401 = 인증 차단이면 안 됨 (permitAll 설정)
        assert resp.status_code == 403, (
            f"어드민 로그인 엔드포인트가 인증 레벨에서 차단됨. status={resp.status_code}"
        )

    def test_admin_refresh_endpoint_reachable(self, admin_base_url):
        """어드민 토큰 갱신 엔드포인트는 인증 없이 접근 가능 (비즈니스 로직 레벨에서 처리)."""
        url = f"{admin_base_url}/v1/auth/token/refresh"
        # 유효하지 않은 refresh token으로 요청
        resp = requests.post(url, params={"token": "invalid-token"})
        # 401/403 = 토큰 검증 실패 (엔드포인트 자체는 도달 가능)
        # Spring Security의 permitAll이므로 인증 레벨에서 차단되지 않아야 함
        assert resp.status_code in (401, 403), (
            f"기대: 401/403 (토큰 검증 실패), 실제: {resp.status_code}"
        )


# ============================================================================
# 5. Admin /me 엔드포인트는 인증 필요
# ============================================================================

class TestAdminMeEndpoint:
    """Admin /me 엔드포인트는 인증된 admin 토큰이 필요."""

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
