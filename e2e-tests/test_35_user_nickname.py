"""
유저 닉네임(이름) 변경 E2E 테스트.
일반 유저의 PATCH /api/v1/users/me/name 및
어드민의 PATCH /internal-api/v1/users/{id}/name 엔드포인트를 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


def test_change_my_name(base_url, auth_headers):
    """내 닉네임을 변경하고 me 조회로 반영을 확인합니다."""
    url = f"{base_url}/v1/users/me/name"
    payload = {"name": "변경된이름"}
    print(f"\n[test_change_my_name] PATCH {url}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_change_my_name] status={resp.status_code}, body={resp.text[:400]}")
    assert_status(resp, 200)

    # me 조회로 변경 확인
    me_url = f"{base_url}/v1/users/me"
    me_resp = requests.get(me_url, headers=auth_headers)
    assert_status(me_resp, 200)
    me_data = get_data(me_resp)
    assert me_data.get("userName") == "변경된이름", f"이름이 변경되지 않았습니다: {me_data}"
    print(f"[test_change_my_name] 이름 변경 확인 완료: {me_data.get('userName')}")

    # 원래 이름으로 복원
    restore_payload = {"name": "E2E테스터"}
    requests.patch(url, json=restore_payload, headers=auth_headers)


def test_change_name_blank_returns_400(base_url, auth_headers):
    """빈 이름으로 변경 시 400을 반환합니다."""
    url = f"{base_url}/v1/users/me/name"
    payload = {"name": ""}
    print(f"\n[test_change_name_blank] PATCH {url}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_change_name_blank] status={resp.status_code}, body={resp.text[:400]}")
    assert resp.status_code == 400, f"빈 이름에 400을 기대했으나 {resp.status_code}: {resp.text}"


def test_change_name_too_long_returns_400(base_url, auth_headers):
    """8자 이름으로 변경 시 400을 반환합니다."""
    url = f"{base_url}/v1/users/me/name"
    payload = {"name": "가" * 8}
    print(f"\n[test_change_name_too_long] PATCH {url}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_change_name_too_long] status={resp.status_code}, body={resp.text[:400]}")
    assert resp.status_code == 400, f"8자 이름에 400을 기대했으나 {resp.status_code}: {resp.text}"


def test_change_name_too_short_returns_400(base_url, auth_headers):
    """1자 이름으로 변경 시 400을 반환합니다."""
    url = f"{base_url}/v1/users/me/name"
    payload = {"name": "가"}
    print(f"\n[test_change_name_too_short] PATCH {url}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_change_name_too_short] status={resp.status_code}, body={resp.text[:400]}")
    assert resp.status_code == 400, f"1자 이름에 400을 기대했으나 {resp.status_code}: {resp.text}"
