"""
DuDoong Backend E2E 테스트 공통 픽스처 및 공유 상태 정의.
모든 테스트 모듈에서 이 파일의 fixtures를 사용합니다.

API 응답은 SuccessResponseAdvice에 의해 {"status":200, "data":{...}} 형태로 래핑됩니다.
get_data() 헬퍼를 사용하여 data 필드를 추출하세요.

DB 직접 접근(역할 승격·시각 이동·상태 확인)은 반드시 e2e_db fixture 하나로 한다 (#737). 접속 정보는 환경변수:
  E2E_DB(기본 dudoong) · E2E_DB_USER(dudoong) · E2E_DB_PASSWORD(dudoong) · E2E_DB_ROOT_PASSWORD(dudoong)
  · E2E_DB_HOST(127.0.0.1) · E2E_DB_PORT(13306)
기본값은 docker-compose.yml 의 로컬 개발용 값이다 (운영 값 아님). 처음 쓸 때 서버가 같은 DB 를 쓰는지 확인하고, 다르면 전체 실행을 멈춘다.
"""
import os
import subprocess
import uuid

import pytest
import requests


class TestState:
    """테스트 모듈 간 공유되는 상태 (세션 전체에서 유지)"""
    access_token: str = ""
    refresh_token: str = ""
    host_id: int = 0
    event_id: int = 0
    ticket_item_id: int = 0
    cart_id: int = 0
    order_uuid: str = ""
    comment_id: int = 0
    # 환불 테스트용 별도 주문
    refund_order_uuid: str = ""


@pytest.fixture(scope="session")
def state():
    """세션 전체에서 공유되는 TestState 인스턴스를 반환합니다."""
    return TestState()


@pytest.fixture(scope="session")
def base_url():
    """API 베이스 URL. 환경변수 API_BASE_URL로 재정의 가능합니다."""
    return os.environ.get("API_BASE_URL", "http://localhost:8080/api")


@pytest.fixture(scope="session")
def auth_token(base_url, state):
    """
    로컬 개발용 로그인을 수행하고 accessToken을 반환합니다.
    세션 스코프이므로 전체 테스트 실행 중 1회만 로그인합니다.
    """
    url = f"{base_url}/v1/auth/oauth/local/login"
    payload = {
        "email": "test@dudoong.com",
        "name": "E2E테스터",
        "phoneNumber": "010-0000-0000",
        "profileImage": None,
        "marketingAgree": False,
    }
    print(f"\n[AUTH] POST {url}")
    resp = requests.post(url, json=payload)
    print(f"[AUTH] status={resp.status_code}, body={resp.text[:300]}")
    assert resp.status_code == 200, f"로그인 실패: {resp.text}"
    data = get_data(resp)
    state.access_token = data["accessToken"]
    state.refresh_token = data["refreshToken"]
    return data["accessToken"]


@pytest.fixture(scope="session")
def auth_headers(auth_token):
    """Authorization Bearer 헤더 딕셔너리를 반환합니다."""
    return {"Authorization": f"Bearer {auth_token}"}


def assert_status(response, expected_status):
    """응답 상태코드를 검증하는 헬퍼 함수입니다."""
    assert response.status_code == expected_status, (
        f"기대 상태코드 {expected_status}, 실제: {response.status_code}\n"
        f"응답 본문: {response.text[:500]}"
    )


def get_data(response):
    """SuccessResponseAdvice 래핑된 응답에서 data 필드를 추출합니다."""
    body = response.json()
    if "data" in body:
        return body["data"]
    return body


# ===== DB 직접 접근 (#737) =====


class E2EDb:
    """E2E 가 SQL 로 접속하는 로컬 MySQL. mysql CLI 를 쓰고 비밀번호는 인자가 아니라 MYSQL_PWD 환경변수로 넘긴다"""

    def __init__(self, name, user, password, root_password, host, port):
        self.name = name
        self.user = user
        self.password = password
        self.root_password = root_password
        self.host = host
        self.port = port

    @classmethod
    def from_env(cls):
        return cls(
            name=os.environ.get("E2E_DB", "dudoong"),
            user=os.environ.get("E2E_DB_USER", "dudoong"),
            password=os.environ.get("E2E_DB_PASSWORD", "dudoong"),
            root_password=os.environ.get("E2E_DB_ROOT_PASSWORD", "dudoong"),
            host=os.environ.get("E2E_DB_HOST", "127.0.0.1"),
            port=os.environ.get("E2E_DB_PORT", "13306"),
        )

    def command(self, *extra, root=False, database=True):
        """mysql CLI 인자 (subprocess.Popen 등에 그대로 쓴다). 비밀번호는 env(root) 로 넘긴다"""
        user = "root" if root else self.user
        args = ["mysql", "-h", self.host, "-P", str(self.port), "-u", user, *extra]
        return args + ([self.name] if database else [])

    def env(self, root=False):
        return {**os.environ, "MYSQL_PWD": self.root_password if root else self.password}

    def run(self, sql, root=False, database=True):
        """실행 결과 (returncode, stdout). 실패를 직접 다루는 곳(권한 확인 등)에서만 쓴다"""
        result = subprocess.run(
            self.command("-N", "-e", sql, root=root, database=database), capture_output=True, text=True, env=self.env(root),
        )
        return result.returncode, result.stdout.strip() if result.returncode == 0 else result.stderr.strip()

    def query(self, sql, root=False, database=True):
        """SQL 실행 후 stdout(탭 구분, 헤더 없음). 실패하면 바로 AssertionError — 조용히 넘어가지 않는다"""
        code, out = self.run(sql, root=root, database=database)
        assert code == 0, f"SQL 실패 (DB={self.name}): {out}\n{sql}"
        return out

    def describe(self):
        return f"{self.user}@{self.host}:{self.port}/{self.name}"


def verify_server_uses(db, base_url):
    """서버(base_url)와 db 가 같은 DB 인지: 서버로 표식 유저를 만들고(로컬 로그인) db 에서 그 유저를 찾는다.
    서버 설정을 읽는 API 가 없어서(운영 코드 변경 없음) 데이터로 확인한다. 반환: 문제 설명(같으면 None)"""
    code, out = db.run("SELECT 1")
    if code != 0:
        return f"DB 에 접속할 수 없음 ({db.describe()}): {out}"
    email = f"e2e-dbcheck-{uuid.uuid4().hex[:12]}@dudoong.com"
    resp = requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={"email": email, "name": "DB확인", "phoneNumber": "010-0000-0000", "profileImage": None, "marketingAgree": False},
    )
    if resp.status_code != 200:
        return f"서버 로컬 로그인 실패 ({base_url}): {resp.status_code} {resp.text[:200]}"
    me = requests.get(f"{base_url}/v1/users/me", headers={"Authorization": f"Bearer {get_data(resp)['accessToken']}"})
    user_id = str(get_data(me)["userId"])
    found = db.query(f"SELECT user_id FROM tbl_user WHERE email = '{email}'")
    if found != user_id:
        return (
            f"서버({base_url})가 만든 유저(user_id={user_id}, {email})가 DB {db.describe()} 에 없음(조회 결과: {found or '없음'}) — "
            f"서버가 다른 DB 를 쓴다. 서버 spring.datasource.url 의 DB 이름과 E2E_DB 를 맞출 것"
        )
    return None


@pytest.fixture(scope="session")
def e2e_db(base_url):
    """DB 직접 접근의 유일한 입구. 첫 사용 때 서버와 같은 DB 인지 확인하고, 다르면 전체 실행을 멈춘다(pytest.exit)"""
    db = E2EDb.from_env()
    problem = verify_server_uses(db, base_url)
    if problem:
        pytest.exit(f"[E2E DB 사전 검사 실패] {problem}", returncode=3)
    return db


@pytest.fixture(scope="session")
def e2e_db_root(e2e_db):
    """performance_schema 등 root 권한이 필요한 테스트용. root 로 읽지 못하면 그 테스트만 skip (사유 표시)"""
    code, out = e2e_db.run("SELECT COUNT(*) FROM performance_schema.data_lock_waits", root=True, database=False)
    if code != 0:
        pytest.skip(f"performance_schema 를 root 로 읽을 수 없음 (E2E_DB_ROOT_PASSWORD 확인): {out[:120]}")
    return e2e_db

