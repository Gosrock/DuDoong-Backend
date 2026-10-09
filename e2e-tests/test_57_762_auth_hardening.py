"""
인증·계정 상태 강화 E2E 테스트 (#762).

시나리오: 개발용 로그인 계정의 oid 접두사 → 운영 정지 계정의 기존 토큰은 익명 취급(보호 경로 401, 공개 GET·로그아웃 200),
refresh 거부, 재로그인 403, 정상 복구 후 재로그인 → ADMIN 은 SUPER_ADMIN·자기 자신의 상태를 바꿀 수 없음 →
활성 호스트 마스터 탈퇴 거절(본인·운영, 탈퇴한 멤버는 세지 않음) → 운영 탈퇴 → 운영 재고 조정의 공연·티켓 소속 검사.

역할 승격·상태 확인은 conftest 의 e2e_db fixture(환경변수 E2E_DB 등, #737)로 한다.
재실행해도 충돌하지 않도록 유저 이메일에 실행마다 다른 접미사를 붙인다.
"""
import base64
import json
import uuid
from datetime import datetime, timedelta

import pytest
import requests

from conftest import assert_status, get_data

RUN = uuid.uuid4().hex[:8]
FMT = "%Y.%m.%d %H:%M"
START = (datetime.now() + timedelta(days=30)).replace(hour=18, minute=0, second=0, microsecond=0)
END = START + timedelta(minutes=120)
PEOPLE = ["admin", "super", "super2", "victim", "master", "member", "solo", "quitter", "dropped"]


class State:
    tokens: dict = {}
    refresh: dict = {}
    emails: dict = {}
    user_ids: dict = {}
    events: dict = {}
    tickets: dict = {}


@pytest.fixture(scope="module")
def s():
    return State()


DB = None


@pytest.fixture(autouse=True, scope="module")
def _bind_db(e2e_db):
    global DB
    DB = e2e_db


def _sql(sql):
    return DB.query(sql)


def _h(s, who):
    return {"Authorization": f"Bearer {s.tokens[who]}"}


def _code(resp):
    return resp.json().get("code")


def _login_resp(base_url, email, name):
    return requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={"email": email, "name": name, "phoneNumber": "010-0000-0000", "profileImage": None, "marketingAgree": False},
    )


def _login(base_url, s, who):
    resp = _login_resp(base_url, s.emails[who], f"강화{who}"[:7])
    assert_status(resp, 200)
    data = get_data(resp)
    s.tokens[who], s.refresh[who] = data["accessToken"], data["refreshToken"]
    part = data["accessToken"].split(".")[1]
    part += "=" * (-len(part) % 4)
    s.user_ids[who] = int(json.loads(base64.urlsafe_b64decode(part))["sub"])


def _admin(base_url):
    return base_url.replace("/api", "/internal-api")


def _set_status(base_url, s, actor, target, status):
    return requests.patch(f"{_admin(base_url)}/v1/users/{s.user_ids[target]}/status", json={"status": status}, headers=_h(s, actor))


def _me(base_url, s, who):
    return requests.get(f"{base_url}/v1/users/me", headers=_h(s, who))


def _state(s, who):
    return _sql(f"SELECT account_state FROM tbl_user WHERE user_id = {s.user_ids[who]}")


def _new_host(base_url, s, who):
    resp = requests.post(f"{base_url}/v2/hosts", json={"name": f"강화{who[:3]}{RUN[:4]}", "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)["hostId"]


def _new_event(base_url, s, host_id, key):
    """준비중 공연 + 무료 티켓 1개 (재고 조정 대상)"""
    resp = requests.post(
        f"{base_url}/v2/events",
        json={"hostId": host_id, "name": f"강화공연{key}", "startAt": START.strftime(FMT), "endAt": END.strftime(FMT), "hasTicket": True},
        headers=_h(s, "master"),
    )
    assert_status(resp, 200)
    event_id = get_data(resp)["eventId"]
    resp = requests.post(f"{base_url}/v2/events/{event_id}/ticket-items", json={
        "payType": "FREE", "name": "무료", "description": "무료", "price": 0, "supplyCount": 100, "account": None,
        "approvalRequired": False, "isQuantityPublic": True, "purchaseLimit": 4, "saleStartAt": None, "saleEndAt": None,
    }, headers=_h(s, "master"))
    assert_status(resp, 200)
    s.events[key], s.tickets[key] = event_id, get_data(resp)["ticketItemId"]


def test_01_setup(base_url, s):
    for who in PEOPLE:
        s.emails[who] = f"auth762-{who}-{RUN}@dudoong.com"
        _login(base_url, s, who)
    _sql(f"UPDATE tbl_user SET account_role='ADMIN' WHERE user_id = {s.user_ids['admin']}")
    _sql(f"UPDATE tbl_user SET account_role='SUPER_ADMIN' WHERE user_id IN ({s.user_ids['super']}, {s.user_ids['super2']})")


def test_02_dev_login_oid_has_local_prefix(s):
    """개발용 로그인 계정은 실제 카카오 회원번호와 겹치지 않는 oid 를 쓴다 (S-1)"""
    assert _sql(f"SELECT oid FROM tbl_user WHERE user_id = {s.user_ids['victim']}") == f"local:{s.emails['victim']}"


def _deleted_cookies(resp):
    """로그아웃 응답이 지우는(Max-Age=0) 쿠키 이름"""
    headers = resp.raw.headers.getlist("Set-Cookie")
    return {h.split("=", 1)[0] for h in headers if "Max-Age=0" in h}


def test_03_suspended_token_is_anonymous(base_url, s):
    """정지 계정의 기존 토큰: 익명 취급 — 보호 경로 401, 공개 GET 200, 로그아웃 200(쿠키 삭제). refresh 삭제, 재로그인 403 (M-2)"""
    assert_status(_me(base_url, s, "victim"), 200)
    assert_status(_set_status(base_url, s, "admin", "victim", "SUSPENDED"), 200)
    cookies = {"accessToken": s.tokens["victim"], "refreshToken": s.refresh["victim"]}

    # 보호 경로: 헤더·쿠키 모두 401
    assert _me(base_url, s, "victim").status_code == 401
    assert requests.get(f"{base_url}/v1/users/me", cookies=cookies).status_code == 401
    # 공개 GET 은 그대로
    assert_status(requests.get(f"{base_url}/v2/home", cookies=cookies), 200)
    assert_status(requests.get(f"{base_url}/v2/tags", headers=_h(s, "victim")), 200)
    # 로그아웃은 200 이고 두 쿠키를 지운다
    resp = requests.post(f"{base_url}/v1/auth/logout", cookies=cookies)
    assert_status(resp, 200)
    assert {"accessToken", "refreshToken"} <= _deleted_cookies(resp), resp.raw.headers.getlist("Set-Cookie")
    # refresh 는 정지 때 지워졌다
    resp = requests.post(f"{base_url}/v1/auth/token/refresh", params={"token": s.refresh["victim"]})
    assert resp.status_code == 403 and _code(resp) == "AUTH_403_1", resp.text
    # 재로그인은 기존 403
    resp = _login_resp(base_url, s.emails["victim"], "강화victim")
    assert resp.status_code == 403 and _code(resp) == "USER_403_1", resp.text

    assert_status(_set_status(base_url, s, "admin", "victim", "NORMAL"), 200)
    _login(base_url, s, "victim")
    assert_status(_me(base_url, s, "victim"), 200)


def test_03b_logout(base_url, s):
    """정상 로그아웃: 쿠키 삭제 + refresh 삭제. 토큰 없이 불러도 200 (쿠키만 지운다)"""
    resp = requests.post(f"{base_url}/v1/auth/logout", headers=_h(s, "victim"))
    assert_status(resp, 200)
    assert {"accessToken", "refreshToken"} <= _deleted_cookies(resp)
    resp = requests.post(f"{base_url}/v1/auth/token/refresh", params={"token": s.refresh["victim"]})
    assert resp.status_code == 403, resp.text
    resp = requests.post(f"{base_url}/v1/auth/logout")
    assert_status(resp, 200)
    assert {"accessToken", "refreshToken"} <= _deleted_cookies(resp)
    _login(base_url, s, "victim")


def test_04_admin_cannot_change_super_admin(base_url, s):
    """ADMIN 이 SUPER_ADMIN 의 상태를 바꾸면 403, SUPER_ADMIN 은 가능 (M-5)"""
    resp = _set_status(base_url, s, "admin", "super2", "SUSPENDED")
    assert resp.status_code == 403 and _code(resp) == "ADMIN_403_2", resp.text
    assert _state(s, "super2") == "NORMAL"

    assert_status(_set_status(base_url, s, "super", "super2", "SUSPENDED"), 200)
    assert _state(s, "super2") == "SUSPENDED"
    assert_status(_set_status(base_url, s, "super", "super2", "NORMAL"), 200)

    # 자기 자신은 바꿀 수 없다
    resp = _set_status(base_url, s, "admin", "admin", "SUSPENDED")
    assert resp.status_code == 400 and _code(resp) == "ADMIN_400_2", resp.text
    assert _state(s, "admin") == "NORMAL"


def test_05_active_host_master_cannot_withdraw(base_url, s):
    """다른 멤버가 있는 호스트의 마스터는 본인 탈퇴·운영 탈퇴 모두 거절, 로그인은 유지 (X-4)"""
    host_id = _new_host(base_url, s, "master")
    resp = requests.post(f"{base_url}/v2/hosts/{host_id}/members", json={"members": [{"email": s.emails["member"], "role": "MANAGER"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    _new_event(base_url, s, host_id, "a")
    _new_event(base_url, s, host_id, "b")

    resp = requests.delete(f"{base_url}/v1/auth/me", headers=_h(s, "master"))
    assert resp.status_code == 400 and _code(resp) == "USER_400_6", resp.text
    assert "양도" in resp.json()["reason"]
    assert_status(_me(base_url, s, "master"), 200)
    assert_status(requests.post(f"{base_url}/v1/auth/token/refresh", params={"token": s.refresh["master"]}), 200)
    _login(base_url, s, "master")

    resp = _set_status(base_url, s, "admin", "master", "DELETED")
    assert resp.status_code == 400 and _code(resp) == "USER_400_6", resp.text
    assert _state(s, "master") == "NORMAL"
    assert_status(_me(base_url, s, "master"), 200)


def test_06_solo_host_master_can_withdraw(base_url, s):
    """혼자이거나 남은 멤버가 모두 탈퇴했고 진행 중인 공연이 없는 호스트의 마스터는 탈퇴할 수 있다"""
    host_id = _new_host(base_url, s, "solo")
    resp = requests.post(f"{base_url}/v2/hosts/{host_id}/members", json={"members": [{"email": s.emails["quitter"], "role": "MANAGER"}]}, headers=_h(s, "solo"))
    assert_status(resp, 200)
    resp = requests.delete(f"{base_url}/v1/auth/me", headers=_h(s, "solo"))
    assert resp.status_code == 400 and _code(resp) == "USER_400_6", resp.text

    assert_status(requests.delete(f"{base_url}/v1/auth/me", headers=_h(s, "quitter")), 200)
    assert_status(requests.delete(f"{base_url}/v1/auth/me", headers=_h(s, "solo")), 200)
    assert _state(s, "solo") == "DELETED"
    assert _me(base_url, s, "solo").status_code == 401


def test_07_admin_withdraw(base_url, s):
    """운영 탈퇴는 정식 탈퇴 절차를 따른다: 상태·oid 정리, 기존 토큰·refresh 거부 (M-5)"""
    assert_status(_set_status(base_url, s, "admin", "dropped", "DELETED"), 200)
    assert _state(s, "dropped") == "DELETED"
    assert _sql(f"SELECT oid FROM tbl_user WHERE user_id = {s.user_ids['dropped']}").startswith("DELETED:")
    assert _me(base_url, s, "dropped").status_code == 401
    resp = requests.post(f"{base_url}/v1/auth/token/refresh", params={"token": s.refresh["dropped"]})
    assert resp.status_code == 403, resp.text
    # 탈퇴는 되돌릴 수 없다: 정상 복구·다시 탈퇴 모두 400
    for status in ("NORMAL", "SUSPENDED", "DELETED"):
        resp = _set_status(base_url, s, "admin", "dropped", status)
        assert resp.status_code == 400 and _code(resp) == "USER_400_7", resp.text
    assert _state(s, "dropped") == "DELETED"


def test_08_admin_adjust_stock_checks_event(base_url, s):
    """운영 재고 조정: 경로의 공연과 티켓 소속이 다르면 400, 재고 그대로 (L-7)"""
    url = lambda event_key, ticket_key: f"{_admin(base_url)}/v1/events/{s.events[event_key]}/ticket-items/{s.tickets[ticket_key]}/adjust-stock"
    before = _sql(f"SELECT quantity FROM tbl_ticket_item WHERE ticket_item_id = {s.tickets['a']}")

    resp = requests.post(url("b", "a"), json={"delta": 5}, headers=_h(s, "admin"))
    assert resp.status_code == 400 and _code(resp) == "Ticket_Item_400_5", resp.text
    assert _sql(f"SELECT quantity FROM tbl_ticket_item WHERE ticket_item_id = {s.tickets['a']}") == before

    assert_status(requests.post(url("a", "a"), json={"delta": 5}, headers=_h(s, "admin")), 200)
    assert int(_sql(f"SELECT quantity FROM tbl_ticket_item WHERE ticket_item_id = {s.tickets['a']}")) == int(before) + 5
