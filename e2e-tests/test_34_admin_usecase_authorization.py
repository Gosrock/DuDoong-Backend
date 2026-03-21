"""
Admin UseCase 이중 권한 체크 E2E 테스트.
- MANAGER: 읽기 OK, 쓰기 403
- ADMIN: 읽기/쓰기 OK, 역할 변경 403
- SUPER_ADMIN: 모든 API OK
"""
import os
import subprocess
import pytest
import requests
from conftest import assert_status, get_data

MYSQL_CMD = "mysql -h 127.0.0.1 -P 13306 -u dudoong -pdudoong dudoong -e"

def mysql_exec(sql):
    result = subprocess.run(
        f'{MYSQL_CMD} "{sql}"', shell=True, capture_output=True, text=True
    )
    return result.stdout

def admin_login(api_base, email, name):
    """어드민 로컬 로그인 (internal-api)"""
    admin_base = api_base.replace("/api", "/internal-api")
    url = f"{admin_base}/v1/auth/oauth/local/login"
    payload = {
        "email": email,
        "name": name,
        "phoneNumber": "010-0000-0000",
        "profileImage": None,
        "marketingAgree": False,
    }
    resp = requests.post(url, json=payload)
    return resp

def get_user_id(api_base, token):
    resp = requests.get(
        f"{api_base}/v1/users/me",
        headers={"Authorization": f"Bearer {token}"},
    )
    if resp.status_code == 200:
        return get_data(resp).get("userId") or get_data(resp).get("id")
    return None

@pytest.fixture(scope="module")
def api_base():
    return os.environ.get("API_BASE_URL", "http://localhost:8080/api")

@pytest.fixture(scope="module")
def admin_base(api_base):
    return api_base.replace("/api", "/internal-api")

@pytest.fixture(scope="module")
def test_user_setup(api_base):
    """테스트용 유저 생성 및 토큰 획득"""
    # 일반 로그인으로 유저 생성
    url = f"{api_base}/v1/auth/oauth/local/login"
    payload = {
        "email": "admin-e2e-test@dudoong.com",
        "name": "어드민E2E",
        "phoneNumber": "010-0000-0000",
        "profileImage": None,
        "marketingAgree": False,
    }
    resp = requests.post(url, json=payload)
    assert resp.status_code == 200
    data = get_data(resp)
    token = data["accessToken"]
    user_id = get_user_id(api_base, token)
    return {"user_id": user_id, "email": "admin-e2e-test@dudoong.com"}


class TestManagerReadOnlyAccess:
    """MANAGER는 어드민 읽기 API만 접근 가능"""

    def test_manager_can_read_dashboard(self, api_base, admin_base, test_user_setup):
        user_id = test_user_setup["user_id"]
        mysql_exec(f"UPDATE tbl_user SET account_role='MANAGER' WHERE user_id={user_id}")

        try:
            # MANAGER로 admin 로그인
            resp = admin_login(api_base, test_user_setup["email"], "어드민E2E")
            assert resp.status_code == 200, f"MANAGER 어드민 로그인 실패: {resp.text}"
            admin_token = get_data(resp)["accessToken"]
            headers = {"X-Admin-Token": admin_token}

            # 읽기 API — 200
            resp = requests.get(f"{admin_base}/v1/dashboard", headers=headers)
            assert_status(resp, 200)

        finally:
            mysql_exec(f"UPDATE tbl_user SET account_role='USER' WHERE user_id={user_id}")

    def test_manager_cannot_delete_comment(self, api_base, admin_base, test_user_setup):
        user_id = test_user_setup["user_id"]
        mysql_exec(f"UPDATE tbl_user SET account_role='MANAGER' WHERE user_id={user_id}")

        try:
            resp = admin_login(api_base, test_user_setup["email"], "어드민E2E")
            assert resp.status_code == 200
            admin_token = get_data(resp)["accessToken"]
            headers = {"X-Admin-Token": admin_token}

            # 쓰기 API — 403 (존재하지 않는 댓글이어도 권한 체크가 먼저)
            resp = requests.delete(f"{admin_base}/v1/comments/99999", headers=headers)
            assert resp.status_code == 403, (
                f"MANAGER가 댓글 삭제 가능. status={resp.status_code}"
            )

        finally:
            mysql_exec(f"UPDATE tbl_user SET account_role='USER' WHERE user_id={user_id}")


class TestAdminWriteAccess:
    """ADMIN은 읽기 + 쓰기 가능, 역할 변경 불가"""

    def test_admin_can_read_and_write(self, api_base, admin_base, test_user_setup):
        user_id = test_user_setup["user_id"]
        mysql_exec(f"UPDATE tbl_user SET account_role='ADMIN' WHERE user_id={user_id}")

        try:
            resp = admin_login(api_base, test_user_setup["email"], "어드민E2E")
            assert resp.status_code == 200
            admin_token = get_data(resp)["accessToken"]
            headers = {"X-Admin-Token": admin_token}

            # 읽기 API — 200
            resp = requests.get(f"{admin_base}/v1/dashboard", headers=headers)
            assert_status(resp, 200)

            # 읽기 API — 유저 목록
            resp = requests.get(f"{admin_base}/v1/users", headers=headers)
            assert_status(resp, 200)

        finally:
            mysql_exec(f"UPDATE tbl_user SET account_role='USER' WHERE user_id={user_id}")

    def test_admin_cannot_change_user_role(self, api_base, admin_base, test_user_setup):
        user_id = test_user_setup["user_id"]
        mysql_exec(f"UPDATE tbl_user SET account_role='ADMIN' WHERE user_id={user_id}")

        try:
            resp = admin_login(api_base, test_user_setup["email"], "어드민E2E")
            assert resp.status_code == 200
            admin_token = get_data(resp)["accessToken"]
            headers = {"X-Admin-Token": admin_token}

            # 역할 변경 API — 403 (SUPER_ADMIN만 가능)
            resp = requests.patch(
                f"{admin_base}/v1/users/{user_id}/role",
                json={"role": "SUPER_ADMIN"},
                headers=headers,
            )
            assert resp.status_code == 403, (
                f"ADMIN이 역할 변경 가능. status={resp.status_code}"
            )

        finally:
            mysql_exec(f"UPDATE tbl_user SET account_role='USER' WHERE user_id={user_id}")


class TestSuperAdminFullAccess:
    """SUPER_ADMIN은 모든 API 접근 가능"""

    def test_super_admin_full_access(self, api_base, admin_base, test_user_setup):
        user_id = test_user_setup["user_id"]
        mysql_exec(f"UPDATE tbl_user SET account_role='SUPER_ADMIN' WHERE user_id={user_id}")

        try:
            resp = admin_login(api_base, test_user_setup["email"], "어드민E2E")
            assert resp.status_code == 200
            admin_token = get_data(resp)["accessToken"]
            headers = {"X-Admin-Token": admin_token}

            # 읽기
            resp = requests.get(f"{admin_base}/v1/dashboard", headers=headers)
            assert_status(resp, 200)

            # 유저 목록
            resp = requests.get(f"{admin_base}/v1/users", headers=headers)
            assert_status(resp, 200)

        finally:
            mysql_exec(f"UPDATE tbl_user SET account_role='USER' WHERE user_id={user_id}")
