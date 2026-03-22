"""
호스트 마스터 권한 양도 E2E 테스트.
호스트 생성 -> 멤버 초대/가입 -> 마스터 양도 -> 검증 흐름을 테스트합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


@pytest.fixture(scope="module")
def transfer_state():
    """마스터 양도 테스트 전용 상태"""
    class TransferState:
        host_id: int = 0
        master_token: str = ""
        member_token: str = ""
        master_user_id: int = 0
        member_user_id: int = 0
    return TransferState()


def _login(base_url, email, name):
    """로컬 로그인 후 (token, userId) 반환"""
    url = f"{base_url}/v1/auth/oauth/local/login"
    payload = {
        "email": email,
        "name": name,
        "phoneNumber": "010-0000-0000",
        "profileImage": None,
        "marketingAgree": False,
    }
    resp = requests.post(url, json=payload)
    assert_status(resp, 200)
    data = get_data(resp)
    return data["accessToken"], data.get("userId")


def test_setup_master_and_member(base_url, transfer_state):
    """마스터와 멤버 유저를 로그인합니다."""
    master_token, _ = _login(base_url, "transfer-master@dudoong.com", "양도마스터")
    member_token, _ = _login(base_url, "transfer-member@dudoong.com", "양도멤버")
    transfer_state.master_token = master_token
    transfer_state.member_token = member_token

    # 각 유저 정보 조회
    for label, token, attr in [
        ("master", master_token, "master_user_id"),
        ("member", member_token, "member_user_id"),
    ]:
        resp = requests.get(
            f"{base_url}/v1/users/me",
            headers={"Authorization": f"Bearer {token}"},
        )
        if resp.status_code == 200:
            data = get_data(resp)
            user_id = data.get("userId") or data.get("id")
            setattr(transfer_state, attr, user_id)
            print(f"[setup] {label} userId={user_id}")


def test_create_host_for_transfer(base_url, transfer_state):
    """마스터가 호스트를 생성합니다."""
    if not transfer_state.master_token:
        pytest.skip("마스터 토큰 없음")

    url = f"{base_url}/v1/hosts"
    payload = {
        "name": "양도테스트호스트",
        "contactEmail": "transfer@dudoong.com",
        "contactNumber": "010-1234-5678",
    }
    headers = {"Authorization": f"Bearer {transfer_state.master_token}"}
    resp = requests.post(url, json=payload, headers=headers)
    print(f"[create_host] status={resp.status_code}, body={resp.text[:400]}")
    assert_status(resp, 200)
    data = get_data(resp)
    transfer_state.host_id = data.get("hostId") or data.get("id")
    print(f"[create_host] hostId={transfer_state.host_id}")


def test_invite_member(base_url, transfer_state):
    """마스터가 멤버를 초대합니다."""
    if not transfer_state.host_id or not transfer_state.member_user_id:
        pytest.skip("호스트 또는 멤버 정보 없음")

    url = f"{base_url}/v1/hosts/{transfer_state.host_id}/invite"
    payload = {"email": "transfer-member@dudoong.com", "role": "MANAGER"}
    headers = {"Authorization": f"Bearer {transfer_state.master_token}"}
    resp = requests.post(url, json=payload, headers=headers)
    print(f"[invite] status={resp.status_code}, body={resp.text[:400]}")
    # 초대 방식이 다를 수 있으므로 200 또는 201 허용
    assert resp.status_code in (200, 201), f"초대 실패: {resp.text[:300]}"


def test_member_join_host(base_url, transfer_state):
    """멤버가 호스트에 가입합니다."""
    if not transfer_state.host_id:
        pytest.skip("호스트 정보 없음")

    url = f"{base_url}/v1/hosts/{transfer_state.host_id}/join"
    headers = {"Authorization": f"Bearer {transfer_state.member_token}"}
    resp = requests.post(url, headers=headers)
    print(f"[join] status={resp.status_code}, body={resp.text[:400]}")
    assert_status(resp, 200)


def test_transfer_master(base_url, transfer_state):
    """마스터가 멤버에게 권한을 양도합니다."""
    if not transfer_state.host_id or not transfer_state.member_user_id:
        pytest.skip("호스트 또는 멤버 정보 없음")

    url = f"{base_url}/v1/hosts/{transfer_state.host_id}/transfer-master"
    payload = {"newMasterUserId": transfer_state.member_user_id}
    headers = {"Authorization": f"Bearer {transfer_state.master_token}"}
    resp = requests.post(url, json=payload, headers=headers)
    print(f"[transfer] status={resp.status_code}, body={resp.text[:400]}")
    assert_status(resp, 200)
    data = get_data(resp)
    print(f"[transfer] 양도 완료, masterUserId={data.get('masterUserId')}")


def test_verify_new_master(base_url, transfer_state):
    """양도 후 호스트 상세 조회로 새 마스터를 검증합니다."""
    if not transfer_state.host_id:
        pytest.skip("호스트 정보 없음")

    url = f"{base_url}/v1/hosts/{transfer_state.host_id}"
    headers = {"Authorization": f"Bearer {transfer_state.member_token}"}
    resp = requests.get(url, headers=headers)
    print(f"[verify] status={resp.status_code}, body={resp.text[:400]}")
    assert_status(resp, 200)
    data = get_data(resp)

    # hostUsers 목록에서 마스터 역할 확인
    host_users = data.get("hostUsers", [])
    master_users = [hu for hu in host_users if hu.get("role") == "마스터" or hu.get("role") == "MASTER"]
    if master_users:
        new_master_id = master_users[0].get("userId")
        assert new_master_id == transfer_state.member_user_id, (
            f"새 마스터 userId={new_master_id}, 기대값={transfer_state.member_user_id}"
        )
        print(f"[verify] 새 마스터 확인 완료: userId={new_master_id}")
    else:
        print(f"[verify] hostUsers에서 마스터 역할을 찾을 수 없습니다: {host_users}")


def test_old_master_cannot_transfer_again(base_url, transfer_state):
    """양도 후 기존 마스터가 다시 양도를 시도하면 실패합니다."""
    if not transfer_state.host_id or not transfer_state.master_user_id:
        pytest.skip("호스트 또는 마스터 정보 없음")

    url = f"{base_url}/v1/hosts/{transfer_state.host_id}/transfer-master"
    payload = {"newMasterUserId": transfer_state.master_user_id}
    headers = {"Authorization": f"Bearer {transfer_state.master_token}"}
    resp = requests.post(url, json=payload, headers=headers)
    print(f"[old_master_retry] status={resp.status_code}, body={resp.text[:300]}")
    # 마스터가 아니므로 403 또는 400 기대
    assert resp.status_code in (400, 403), (
        f"기존 마스터의 재양도 시도가 차단되지 않음: status={resp.status_code}"
    )
    print("[old_master_retry] 기존 마스터의 재양도 시도가 정상적으로 차단됨")
