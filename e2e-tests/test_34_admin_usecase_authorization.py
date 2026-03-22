"""
Admin UseCase 이중 권한 체크 E2E 테스트.
- USER: admin API 접근 403
- ADMIN: 읽기/쓰기 OK, 역할 변경 403
- SUPER_ADMIN: 모든 API OK

NOTE: DB에서 직접 user_id를 조회하여 role을 변경합니다 (me API의 userId와 DB user_id 불일치 방지).
"""
import os
import subprocess
import pytest
import requests
from conftest import assert_status, get_data


def mysql_query(sql):
    """MySQL 쿼리 실행 후 stdout 반환"""
    result = subprocess.run(
        ["mysql", "-h", "127.0.0.1", "-P", "13306", "-u", "dudoong", "-pdudoong",
         "dudoong", "-N", "-e", sql],
        capture_output=True, text=True
    )
    return result.stdout.strip()


def mysql_exec(sql):
    """MySQL 실행 (결과 불필요)"""
    subprocess.run(
        ["mysql", "-h", "127.0.0.1", "-P", "13306", "-u", "dudoong", "-pdudoong",
         "dudoong", "-e", sql],
        capture_output=True, text=True
    )


@pytest.fixture(scope="module")
def api_base():
    return os.environ.get("API_BASE_URL", "http://localhost:8080/api")


@pytest.fixture(scope="module")
def admin_base(api_base):
    return api_base.replace("/api", "/internal-api")


@pytest.fixture(scope="module")
def test_user(api_base):
    """일반 로그인으로 토큰 획득 + DB에서 user_id를 이메일 기반으로 직접 조회"""
    email = "admin@dudoong.com"
    url = f"{api_base}/v1/auth/oauth/local/login"
    payload = {
        "email": email,
        "name": "어드민E2E",
        "phoneNumber": "010-0000-0000",
        "profileImage": None,
        "marketingAgree": False,
    }
    resp = requests.post(url, json=payload)
    assert resp.status_code == 200, f"로그인 실패: {resp.text}"
    token = get_data(resp)["accessToken"]

    # JWT sub에서 userId 추출
    import base64, json
    payload_part = token.split(".")[1]
    payload_part += "=" * (4 - len(payload_part) % 4)
    jwt_payload = json.loads(base64.b64decode(payload_part))
    user_id = int(jwt_payload["sub"])
    print(f"[test_user] email={email}, jwt_sub={user_id}")

    return {"token": token, "user_id": user_id}


class TestUserBlocked:
    """일반 USER는 admin API 접근 불가"""

    def test_user_cannot_access_dashboard(self, admin_base, test_user):
        headers = {"Authorization": f"Bearer {test_user['token']}"}
        resp = requests.get(f"{admin_base}/v1/dashboard", headers=headers)
        assert resp.status_code == 403, f"USER가 dashboard 접근 가능. status={resp.status_code}"


class TestAdminAccess:
    """ADMIN은 읽기/쓰기 가능, 역할 변경 불가"""

    def test_admin_can_read_dashboard(self, admin_base, test_user):
        user_id = test_user["user_id"]
        mysql_exec(f"UPDATE tbl_user SET account_role='ADMIN' WHERE user_id={user_id}")
        try:
            headers = {"Authorization": f"Bearer {test_user['token']}"}
            resp = requests.get(f"{admin_base}/v1/dashboard", headers=headers)
            assert_status(resp, 200)
        finally:
            mysql_exec(f"UPDATE tbl_user SET account_role='USER' WHERE user_id={user_id}")

    def test_admin_can_read_users(self, admin_base, test_user):
        user_id = test_user["user_id"]
        mysql_exec(f"UPDATE tbl_user SET account_role='ADMIN' WHERE user_id={user_id}")
        try:
            headers = {"Authorization": f"Bearer {test_user['token']}"}
            resp = requests.get(f"{admin_base}/v1/users", headers=headers)
            assert_status(resp, 200)
        finally:
            mysql_exec(f"UPDATE tbl_user SET account_role='USER' WHERE user_id={user_id}")

    def test_admin_cannot_change_user_role(self, admin_base, test_user):
        user_id = test_user["user_id"]
        mysql_exec(f"UPDATE tbl_user SET account_role='ADMIN' WHERE user_id={user_id}")
        try:
            headers = {"Authorization": f"Bearer {test_user['token']}"}
            resp = requests.patch(
                f"{admin_base}/v1/users/{user_id}/role",
                json={"role": "SUPER_ADMIN"},
                headers=headers,
            )
            assert resp.status_code == 403, f"ADMIN이 역할 변경 가능. status={resp.status_code}"
        finally:
            mysql_exec(f"UPDATE tbl_user SET account_role='USER' WHERE user_id={user_id}")


class TestSuperAdminFullAccess:
    """SUPER_ADMIN은 모든 API 접근 가능"""

    def test_super_admin_full_access(self, admin_base, test_user):
        user_id = test_user["user_id"]
        mysql_exec(f"UPDATE tbl_user SET account_role='SUPER_ADMIN' WHERE user_id={user_id}")
        try:
            headers = {"Authorization": f"Bearer {test_user['token']}"}

            # 읽기
            resp = requests.get(f"{admin_base}/v1/dashboard", headers=headers)
            assert_status(resp, 200)

            # 유저 목록
            resp = requests.get(f"{admin_base}/v1/users", headers=headers)
            assert_status(resp, 200)
        finally:
            mysql_exec(f"UPDATE tbl_user SET account_role='USER' WHERE user_id={user_id}")
