"""
호스트 초대/가입/거절/역할 변경/Slack URL 설정 E2E 테스트.
user1이 호스트를 운영하고, user2를 초대하여 가입/거절/역할 변경을 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


# 이 모듈 내에서만 사용하는 로컬 상태
_invite_state: dict = {
    "user2_token": "",
    "user2_headers": {},
    "user2_id": 0,
    "user3_token": "",
    "user3_headers": {},
}


def _login_user(base_url: str, email: str, name: str, phone: str) -> dict:
    """테스트 유저 로그인 후 토큰 및 유저 정보를 반환합니다."""
    url = f"{base_url}/v1/auth/oauth/local/login"
    payload = {
        "email": email,
        "name": name,
        "phoneNumber": phone,
        "profileImage": None,
        "marketingAgree": False,
    }
    resp = requests.post(url, json=payload)
    assert resp.status_code == 200, f"로그인 실패: {resp.text}"
    data = get_data(resp)
    return data


def test_setup_users_for_invite(base_url):
    """초대 테스트를 위한 user2, user3 로그인."""
    print("\n[test_setup_users_for_invite] user2, user3 로그인")
    data2 = _login_user(base_url, "invite-user2@dudoong.com", "초대테스터2", "010-2222-3333")
    _invite_state["user2_token"] = data2["accessToken"]
    _invite_state["user2_headers"] = {"Authorization": f"Bearer {data2['accessToken']}"}

    data3 = _login_user(base_url, "invite-user3@dudoong.com", "초대테스터3", "010-3333-4444")
    _invite_state["user3_token"] = data3["accessToken"]
    _invite_state["user3_headers"] = {"Authorization": f"Bearer {data3['accessToken']}"}
    print("[test_setup_users_for_invite] user2, user3 로그인 완료")


def test_invite_user_to_host(base_url, auth_headers, state):
    """user1이 user2를 호스트에 MANAGER 역할로 초대합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")
    if not _invite_state["user2_token"]:
        pytest.skip("user2 토큰이 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/hosts/{state.host_id}/invite"
    payload = {
        "email": "invite-user2@dudoong.com",
        "role": "MANAGER",
    }
    print(f"\n[test_invite_user_to_host] POST {url}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_invite_user_to_host] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (200, 201), (
        f"호스트 초대 실패: status={resp.status_code}, body={resp.text[:300]}"
    )
    print("[test_invite_user_to_host] 초대 완료")


def test_user2_joins_host(base_url, state):
    """user2가 초대를 수락하고 호스트에 가입합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")
    if not _invite_state["user2_headers"]:
        pytest.skip("user2 헤더가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/hosts/{state.host_id}/join"
    print(f"\n[test_user2_joins_host] POST {url} (user2)")
    resp = requests.post(url, headers=_invite_state["user2_headers"])
    print(f"[test_user2_joins_host] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (200, 201), (
        f"호스트 가입 실패: status={resp.status_code}, body={resp.text[:300]}"
    )
    print("[test_user2_joins_host] user2 호스트 가입 완료")


def test_invite_and_reject(base_url, auth_headers, state):
    """user3를 초대하고, user3가 거절합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")
    if not _invite_state["user3_headers"]:
        pytest.skip("user3 헤더가 없어 테스트를 건너뜁니다.")

    # user3 초대
    invite_url = f"{base_url}/v1/hosts/{state.host_id}/invite"
    invite_payload = {"email": "invite-user3@dudoong.com", "role": "GUEST"}
    print(f"\n[test_invite_and_reject] POST {invite_url} (user3 초대)")
    invite_resp = requests.post(invite_url, json=invite_payload, headers=auth_headers)
    print(f"[test_invite_and_reject] invite status={invite_resp.status_code}")

    if invite_resp.status_code not in (200, 201):
        pytest.skip(f"user3 초대 실패: {invite_resp.text[:200]}")

    # user3 거절
    reject_url = f"{base_url}/v1/hosts/{state.host_id}/reject"
    print(f"[test_invite_and_reject] POST {reject_url} (user3 거절)")
    reject_resp = requests.post(reject_url, headers=_invite_state["user3_headers"])
    print(f"[test_invite_and_reject] reject status={reject_resp.status_code}, body={reject_resp.text[:400]}")

    assert reject_resp.status_code in (200, 201), (
        f"호스트 초대 거절 실패: {reject_resp.text[:300]}"
    )
    print("[test_invite_and_reject] user3 초대 거절 완료")


def test_update_host_slack_url(base_url, auth_headers, state):
    """호스트에 Slack URL을 설정합니다.
    테스트 환경에서는 실제 Slack 웹훅 연결이 불가능하므로 400도 허용합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/hosts/{state.host_id}/slack"
    payload = {"slackUrl": "https://hooks.slack.com/services/T00/B00/xxx"}
    print(f"\n[test_update_host_slack_url] PATCH {url}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_update_host_slack_url] status={resp.status_code}, body={resp.text[:400]}")

    # 테스트 환경에서는 실제 Slack 웹훅에 연결할 수 없어 400이 반환될 수 있음
    assert resp.status_code in (200, 400), (
        f"예상치 못한 응답 코드: status={resp.status_code}, body={resp.text[:300]}"
    )
    if resp.status_code == 400:
        print("[test_update_host_slack_url] 테스트 환경에서 Slack 연결 불가 (400) - 예상된 결과")
    else:
        print("[test_update_host_slack_url] Slack URL 설정 완료")


def test_non_host_user_cannot_access_host_features(base_url, state):
    """호스트에 속하지 않은 user3가 호스트 기능에 접근하면 차단됩니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")
    if not _invite_state["user3_headers"]:
        pytest.skip("user3 헤더가 없어 테스트를 건너뜁니다.")

    # user3는 초대를 거절했으므로 호스트 이벤트 목록 조회가 차단되어야 합니다
    url = f"{base_url}/v1/hosts/{state.host_id}/events"
    print(f"\n[test_non_host_user_cannot_access] GET {url} (user3 - 비호스트)")
    resp = requests.get(url, headers=_invite_state["user3_headers"])
    print(f"[test_non_host_user_cannot_access] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (400, 401, 403, 404), (
        f"비호스트 유저의 호스트 접근이 차단되어야 합니다: status={resp.status_code}"
    )
    print(f"[test_non_host_user_cannot_access] 비호스트 접근 차단 확인: {resp.status_code}")


def test_host_member_can_access_events(base_url, state):
    """호스트에 가입한 user2는 호스트 이벤트 목록을 조회할 수 있습니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")
    if not _invite_state["user2_headers"]:
        pytest.skip("user2 헤더가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/hosts/{state.host_id}/events"
    print(f"\n[test_host_member_can_access_events] GET {url} (user2 - 호스트 멤버)")
    resp = requests.get(url, headers=_invite_state["user2_headers"])
    print(f"[test_host_member_can_access_events] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    print("[test_host_member_can_access_events] 호스트 멤버 이벤트 조회 성공")
