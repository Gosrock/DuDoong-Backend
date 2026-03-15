"""
DuDoong Backend E2E 테스트 공통 픽스처 및 공유 상태 정의.
모든 테스트 모듈에서 이 파일의 fixtures를 사용합니다.

API 응답은 SuccessResponseAdvice에 의해 {"status":200, "data":{...}} 형태로 래핑됩니다.
get_data() 헬퍼를 사용하여 data 필드를 추출하세요.
"""
import os
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
