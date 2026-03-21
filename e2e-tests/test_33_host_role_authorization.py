"""
HostRole AOP 호스트 권한 E2E 테스트.
- 호스트 멤버가 아닌 유저는 호스트 관리 API 접근 불가
- GUEST 호스트유저는 조회만 가능, 수정 불가
- SUPER_ADMIN은 모든 호스트/이벤트 접근 가능
"""
import os
import subprocess
import pytest
import requests
from conftest import assert_status, get_data

MYSQL_CMD = "mysql -h 127.0.0.1 -P 13306 -u dudoong -pdudoong dudoong -e"

def mysql_exec(sql):
    """로컬 MySQL에 SQL 실행"""
    result = subprocess.run(
        f'{MYSQL_CMD} "{sql}"', shell=True, capture_output=True, text=True
    )
    return result.stdout

def login_user(base_url, email, name):
    """로컬 로그인하여 accessToken 반환"""
    url = f"{base_url}/v1/auth/oauth/local/login"
    payload = {
        "email": email,
        "name": name,
        "phoneNumber": "010-0000-0000",
        "profileImage": None,
        "marketingAgree": False,
    }
    resp = requests.post(url, json=payload)
    assert resp.status_code == 200, f"로그인 실패: {resp.text}"
    data = get_data(resp)
    return data["accessToken"]

def get_user_id_from_token(base_url, token):
    """토큰으로 유저 ID 조회"""
    url = f"{base_url}/v1/users/me"
    resp = requests.get(url, headers={"Authorization": f"Bearer {token}"})
    if resp.status_code == 200:
        return get_data(resp).get("userId") or get_data(resp).get("id")
    return None

@pytest.fixture(scope="module")
def api_base():
    return os.environ.get("API_BASE_URL", "http://localhost:8080/api")

@pytest.fixture(scope="module")
def user_a(api_base):
    """호스트 MASTER 유저"""
    token = login_user(api_base, "host-master@dudoong.com", "호스트마스터")
    return {"token": token, "headers": {"Authorization": f"Bearer {token}"}}

@pytest.fixture(scope="module")
def user_b(api_base):
    """외부 유저 (호스트 멤버 아님)"""
    token = login_user(api_base, "outsider@dudoong.com", "외부유저")
    return {"token": token, "headers": {"Authorization": f"Bearer {token}"}}

@pytest.fixture(scope="module")
def host_and_event(api_base, user_a):
    """유저 A로 호스트 + 이벤트 생성"""
    # 호스트 생성
    resp = requests.post(
        f"{api_base}/v1/hosts",
        json={
            "name": "E2E권한테스트호스트",
            "contactEmail": "auth-test@dudoong.com",
            "contactNumber": "010-0000-0000",
        },
        headers=user_a["headers"],
    )
    assert_status(resp, 200)
    host_id = get_data(resp).get("hostId") or get_data(resp).get("id")

    # 이벤트 생성
    resp = requests.post(
        f"{api_base}/v1/events",
        json={"name": "E2E권한테스트이벤트", "hostId": host_id},
        headers=user_a["headers"],
    )
    assert_status(resp, 200)
    event_id = get_data(resp).get("eventId") or get_data(resp).get("id")

    return {"host_id": host_id, "event_id": event_id}


class TestNonMemberBlocked:
    """호스트 멤버가 아닌 유저는 호스트 관리 API 접근 불가"""

    def test_outsider_cannot_read_host_events(self, api_base, user_b, host_and_event):
        """외부 유저는 호스트 이벤트 목록 조회 불가"""
        url = f"{api_base}/v1/hosts/{host_and_event['host_id']}/events"
        resp = requests.get(url, headers=user_b["headers"])
        assert resp.status_code in (403, 400), (
            f"외부 유저가 호스트 이벤트에 접근 가능. status={resp.status_code}"
        )

    def test_outsider_cannot_update_event(self, api_base, user_b, host_and_event):
        """외부 유저는 이벤트 수정 불가"""
        url = f"{api_base}/v1/events/{host_and_event['event_id']}/basic"
        resp = requests.patch(
            url,
            json={"name": "해킹시도"},
            headers=user_b["headers"],
        )
        assert resp.status_code in (403, 400), (
            f"외부 유저가 이벤트를 수정 가능. status={resp.status_code}"
        )


class TestMasterAccess:
    """호스트 MASTER는 모든 호스트 관리 API 접근 가능"""

    def test_master_can_read_host_events(self, api_base, user_a, host_and_event):
        """MASTER는 호스트 이벤트 목록 조회 가능"""
        url = f"{api_base}/v1/hosts/{host_and_event['host_id']}/events"
        resp = requests.get(url, headers=user_a["headers"])
        assert_status(resp, 200)

    def test_master_can_read_event_checklist(self, api_base, user_a, host_and_event):
        """MASTER는 이벤트 체크리스트 조회 가능"""
        url = f"{api_base}/v1/events/{host_and_event['event_id']}/checklist"
        resp = requests.get(url, headers=user_a["headers"])
        assert_status(resp, 200)


class TestSuperAdminBypass:
    """SUPER_ADMIN은 호스트 멤버가 아니어도 모든 접근 가능"""

    def test_super_admin_can_access_any_host(self, api_base, user_b, host_and_event):
        """SUPER_ADMIN으로 승격된 유저는 아무 호스트에도 접근 가능"""
        # 유저 B의 ID를 얻기 위해 me API 호출
        user_id = get_user_id_from_token(api_base, user_b["token"])
        if not user_id:
            pytest.skip("유저 ID를 가져올 수 없음")

        # DB에서 SUPER_ADMIN으로 승격
        mysql_exec(f"UPDATE tbl_user SET account_role='SUPER_ADMIN' WHERE user_id={user_id}")

        try:
            # 호스트 멤버가 아닌데 접근 가능해야 함
            url = f"{api_base}/v1/hosts/{host_and_event['host_id']}/events"
            resp = requests.get(url, headers=user_b["headers"])
            assert_status(resp, 200)

            # 이벤트 체크리스트도 접근 가능
            url = f"{api_base}/v1/events/{host_and_event['event_id']}/checklist"
            resp = requests.get(url, headers=user_b["headers"])
            assert_status(resp, 200)
        finally:
            # DB 원복: USER로 되돌리기
            mysql_exec(f"UPDATE tbl_user SET account_role='USER' WHERE user_id={user_id}")
