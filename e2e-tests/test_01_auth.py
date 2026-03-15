"""
인증 관련 E2E 시나리오 테스트.
로컬 개발용 로그인, 토큰 갱신, 헬스체크, 미인증 접근 검증을 포함합니다.
"""
import pytest
import requests

from conftest import assert_status


def test_local_login(base_url, state):
    """로컬 개발용 즉시 로그인이 성공하고 accessToken/refreshToken이 반환되는지 확인합니다."""
    url = f"{base_url}/v1/auth/oauth/local/login"
    payload = {
        "email": "test@dudoong.com",
        "name": "E2E테스터",
        "phoneNumber": "010-0000-0000",
        "profileImage": None,
        "marketingAgree": False,
    }
    print(f"\n[test_local_login] POST {url}")
    resp = requests.post(url, json=payload)
    print(f"[test_local_login] status={resp.status_code}, body={resp.text[:300]}")

    assert_status(resp, 200)
    data = resp.json()
    assert "accessToken" in data, "accessToken 필드가 응답에 없습니다"
    assert "refreshToken" in data, "refreshToken 필드가 응답에 없습니다"
    assert data["accessToken"], "accessToken이 비어 있습니다"
    assert data["refreshToken"], "refreshToken이 비어 있습니다"

    # state에 저장 (다른 테스트에서 재사용)
    state.access_token = data["accessToken"]
    state.refresh_token = data["refreshToken"]
    print(f"[test_local_login] accessToken 획득 완료: {data['accessToken'][:30]}...")


def test_token_refresh(base_url, state):
    """refreshToken으로 토큰을 갱신하면 새로운 토큰이 반환되는지 확인합니다."""
    assert state.refresh_token, "refresh_token이 없습니다. test_local_login을 먼저 실행하세요."
    url = f"{base_url}/v1/auth/token/refresh"
    params = {"token": state.refresh_token}
    print(f"\n[test_token_refresh] POST {url} ?token=***")
    resp = requests.post(url, params=params)
    print(f"[test_token_refresh] status={resp.status_code}, body={resp.text[:300]}")

    assert_status(resp, 200)
    data = resp.json()
    assert "accessToken" in data, "accessToken 필드가 응답에 없습니다"
    assert "refreshToken" in data, "refreshToken 필드가 응답에 없습니다"
    # 갱신된 토큰으로 state 업데이트
    state.access_token = data["accessToken"]
    state.refresh_token = data["refreshToken"]
    print(f"[test_token_refresh] 토큰 갱신 완료")


def test_unauthenticated_access(base_url):
    """인증 토큰 없이 보호된 엔드포인트에 접근하면 401 또는 403이 반환되는지 확인합니다."""
    url = f"{base_url}/v1/hosts"
    print(f"\n[test_unauthenticated_access] GET {url} (토큰 없음)")
    resp = requests.get(url)
    print(f"[test_unauthenticated_access] status={resp.status_code}")

    assert resp.status_code in (401, 403), (
        f"미인증 접근 시 401 또는 403이 기대되지만 {resp.status_code}가 반환되었습니다"
    )
    print(f"[test_unauthenticated_access] 미인증 접근 차단 확인: {resp.status_code}")
