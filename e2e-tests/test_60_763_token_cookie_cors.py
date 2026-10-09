"""
토큰 전달·쿠키·CORS·감사 기록 E2E 테스트 (#763).

시나리오: 로그인 쿠키 속성 → refresh 를 본문·쿠키·쿼리(전환 기간)로 → 카카오 링크의 state·redirect_uri 허용 목록 →
쿠키 인증 상태 변경 요청의 Origin 검사(잘못된 Origin·Origin 없음 403, 허용 Origin·헤더 인증 통과, refresh·logout 제외) →
운영 어드민 변경·엑셀 반출의 감사 기록(tbl_admin_audit_log) → SUPER_ADMIN 의 v1 공연 생성(v2 와 같은 예외).

Origin 검사는 기본 report-only(auth.origin-check.enforce=false)다. 서버를 --auth.origin-check.enforce=true 로 띄우고
E2E_ORIGIN_CHECK_ENFORCE=1 을 주면 차단(403 AUTH_403_3)을, 아니면 통과(경고 로그만)를 확인한다.
운영 프로필 확인(CORS 에 localhost 없음, 운영 쿠키 속성)은 운영 프로필로 띄운 두 번째 서버가 있어야 한다:
  E2E_PROD_PROFILE_BASE_URL (예: http://localhost:18764/api) — 없으면 그 테스트만 skip (사유 표시).
  E2E_REQUIRE_PROD_PROFILE=1 이면 skip 대신 실패.
역할 승격·감사 기록 확인은 conftest 의 e2e_db fixture(#737)로 한다. 재실행해도 충돌하지 않도록 이메일에 실행마다 다른 접미사를 붙인다.
"""
import base64
import json
import os
import re
import uuid
from datetime import datetime, timedelta

import pytest
import requests

from conftest import assert_status, get_data

RUN = uuid.uuid4().hex[:8]
FMT = "%Y.%m.%d %H:%M"
START = (datetime.now() + timedelta(days=30)).replace(hour=18, minute=0, second=0, microsecond=0)
PEOPLE = ["user", "admin", "target", "super", "master"]
ENFORCE = os.environ.get("E2E_ORIGIN_CHECK_ENFORCE") == "1"
ALLOWED_LOCAL_ORIGIN = "http://localhost:3000"
EVIL_ORIGIN = "https://evil.example"


class State:
    tokens: dict = {}
    refresh: dict = {}
    user_ids: dict = {}
    login_set_cookies: list = []


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
    try:
        return resp.json().get("code")
    except ValueError:
        return None


def _admin(base_url):
    return base_url.replace("/api", "/internal-api")


def _cookie_attrs(header):
    """Set-Cookie 한 줄 → (이름, {속성 소문자: 값})"""
    parts = [p.strip() for p in header.split(";")]
    name = parts[0].split("=", 1)[0]
    attrs = {}
    for p in parts[1:]:
        k, _, v = p.partition("=")
        attrs[k.lower()] = v
    return name, attrs


def _set_cookies(resp):
    return [_cookie_attrs(h) for h in resp.raw.headers.getlist("Set-Cookie")]


def _login(base_url, s, who):
    resp = requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={"email": f"t763-{who}-{RUN}@dudoong.com", "name": f"t763{who}"[:7], "phoneNumber": "010-0000-0000",
              "profileImage": None, "marketingAgree": False},
    )
    assert_status(resp, 200)
    data = get_data(resp)
    s.tokens[who], s.refresh[who] = data["accessToken"], data["refreshToken"]
    part = data["accessToken"].split(".")[1]
    part += "=" * (-len(part) % 4)
    s.user_ids[who] = int(json.loads(base64.urlsafe_b64decode(part))["sub"])
    return resp


def _new_host(base_url, headers=None, cookies=None, extra_headers=None):
    return requests.post(
        f"{base_url}/v2/hosts",
        json={"name": f"t763{uuid.uuid4().hex[:6]}", "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]},
        headers={**(headers or {}), **(extra_headers or {})},
        cookies=cookies,
    )


def test_01_setup(base_url, s):
    for who in PEOPLE:
        resp = _login(base_url, s, who)
        if who == "user":
            s.login_set_cookies = _set_cookies(resp)
    _sql(f"UPDATE tbl_user SET account_role='ADMIN' WHERE user_id = {s.user_ids['admin']}")
    _sql(f"UPDATE tbl_user SET account_role='SUPER_ADMIN' WHERE user_id = {s.user_ids['super']}")


# ===== 2·3. 쿠키 속성 (로컬 프로필) =====

def test_02_login_cookie_attributes_local(s):
    """로컬 프로필: domain 없음, SameSite=None, Secure, HttpOnly 없음(플래그 기본값 꺼짐 — 프론트가 JS 로 읽는다)"""
    cookies = dict(s.login_set_cookies)
    assert {"accessToken", "refreshToken"} <= set(cookies), s.login_set_cookies
    for name in ("accessToken", "refreshToken"):
        attrs = cookies[name]
        assert "domain" not in attrs, attrs
        assert attrs.get("samesite") == "None", attrs
        assert "secure" in attrs and "httponly" not in attrs, attrs
        assert attrs.get("path") == "/", attrs


# ===== 1. refresh: 본문 / 쿠키 / 쿼리(전환 기간) =====

def test_03_refresh_by_body(base_url, s):
    resp = requests.post(f"{base_url}/v1/auth/token/refresh", json={"refreshToken": s.refresh["user"]})
    assert_status(resp, 200)
    data = get_data(resp)
    s.tokens["user"], s.refresh["user"] = data["accessToken"], data["refreshToken"]


def test_04_refresh_by_cookie_without_origin(base_url, s):
    """프론트 SSR 처럼 Origin 없이 쿠키(accessToken 포함)로 refresh — Origin 검사 제외"""
    resp = requests.post(f"{base_url}/v1/auth/token/refresh",
                         cookies={"accessToken": s.tokens["user"], "refreshToken": s.refresh["user"]})
    assert_status(resp, 200)
    data = get_data(resp)
    s.tokens["user"], s.refresh["user"] = data["accessToken"], data["refreshToken"]


def test_05_refresh_by_query_still_works(base_url, s):
    """기존 ?token= 방식도 전환 기간 동안 된다 (서버는 deprecated 경고 로그만 남김)"""
    resp = requests.post(f"{base_url}/v1/auth/token/refresh", params={"token": s.refresh["user"]})
    assert_status(resp, 200)
    data = get_data(resp)
    s.tokens["user"], s.refresh["user"] = data["accessToken"], data["refreshToken"]


def test_05b_refresh_query_with_form_content_type(base_url, s):
    """쿼리 방식 호환: form Content-Type 에 빈 본문이어도 415 가 아니다"""
    resp = requests.post(f"{base_url}/v1/auth/token/refresh?token={s.refresh['user']}",
                         headers={"Content-Type": "application/x-www-form-urlencoded"})
    assert_status(resp, 200)
    data = get_data(resp)
    s.tokens["user"], s.refresh["user"] = data["accessToken"], data["refreshToken"]


def test_06_refresh_without_token_fails(base_url):
    resp = requests.post(f"{base_url}/v1/auth/token/refresh", json={})
    assert resp.status_code in (400, 401, 403, 404), resp.text


# ===== 7. 카카오 링크: redirect_uri 허용 목록 + state =====

def test_07_kakao_link_state_and_allowlist(base_url):
    resp = requests.get(f"{base_url}/v1/auth/oauth/kakao/link", headers={"Referer": "http://localhost:5173/"})
    assert_status(resp, 200)
    link = get_data(resp)["link"]
    assert "redirect_uri=http://localhost:5173/admin/kakao/callback" in link, link
    state = re.search(r"[?&]state=([A-Za-z0-9_-]{43})", link)
    assert state, link
    cookies = dict(_set_cookies(resp))
    assert "oauth_state" in cookies, resp.raw.headers.getlist("Set-Cookie")
    attrs = cookies["oauth_state"]
    assert "httponly" in attrs and "secure" in attrs and attrs.get("samesite") == "Lax", attrs
    assert resp.cookies.get("oauth_state") is None or resp.cookies.get("oauth_state") == state.group(1)

    evil = requests.get(f"{base_url}/v1/auth/oauth/kakao/link", headers={"Referer": "https://evil.example/admin"})
    assert_status(evil, 200)
    assert "redirect_uri=http://localhost:3000/kakao/callback" in get_data(evil)["link"]


def test_08_kakao_code_state_mismatch_rejected(base_url):
    """state 와 쿠키가 다르면 카카오 호출 전에 400 AUTH_400_1"""
    resp = requests.get(f"{base_url}/v1/auth/oauth/kakao", params={"code": "x", "state": "a" * 43},
                        cookies={"oauth_state": "b" * 43})
    assert resp.status_code == 400 and _code(resp) == "AUTH_400_1", resp.text


# ===== 5. 쿠키 인증 상태 변경 요청의 Origin =====

def test_09_cookie_post_with_wrong_origin_is_403(base_url, s):
    """허용 안 된 Origin 은 CORS 단계에서 403 (모드와 무관)"""
    resp = _new_host(base_url, cookies={"accessToken": s.tokens["user"]}, extra_headers={"Origin": EVIL_ORIGIN})
    assert resp.status_code == 403, resp.text


def test_10_cookie_post_without_origin(base_url, s):
    """Origin·Referer 없음 / 허용 안 된 Referer: 차단 모드면 403 AUTH_403_3, report-only 면 통과"""
    for extra in (None, {"Referer": f"{EVIL_ORIGIN}/x"}):
        resp = _new_host(base_url, cookies={"accessToken": s.tokens["user"]}, extra_headers=extra)
        if ENFORCE:
            assert resp.status_code == 403 and _code(resp) == "AUTH_403_3", resp.text
        else:
            assert_status(resp, 200)


def test_11_cookie_post_with_allowed_origin_passes(base_url, s):
    assert_status(_new_host(base_url, cookies={"accessToken": s.tokens["user"]}, extra_headers={"Origin": ALLOWED_LOCAL_ORIGIN}), 200)
    assert_status(_new_host(base_url, cookies={"accessToken": s.tokens["user"]}, extra_headers={"Referer": f"{ALLOWED_LOCAL_ORIGIN}/admin"}), 200)


def test_12_header_auth_is_not_checked(base_url, s):
    """Bearer 헤더 인증은 Origin 없이 통과 (쿠키가 함께 와도). 쿠키와 헤더가 다른 사용자면 헤더 사용자로 인증"""
    assert_status(_new_host(base_url, headers=_h(s, "user")), 200)
    assert_status(_new_host(base_url, headers=_h(s, "user"), cookies={"accessToken": s.tokens["user"]}), 200)
    me = requests.get(f"{base_url}/v1/users/me", headers=_h(s, "master"), cookies={"accessToken": s.tokens["user"]})
    assert_status(me, 200)
    assert get_data(me)["userId"] == s.user_ids["master"], me.text


def test_13_logout_by_cookie_without_origin(base_url, s):
    _login(base_url, s, "user")
    resp = requests.post(f"{base_url}/v1/auth/logout", cookies={"accessToken": s.tokens["user"], "refreshToken": s.refresh["user"]})
    assert_status(resp, 200)
    _login(base_url, s, "user")


# ===== 8. 운영 어드민 감사 기록 =====

def _audit_rows(actor_id):
    out = _sql(
        "SELECT action, http_method, request_path, IFNULL(target,''), IFNULL(request_detail,''), IFNULL(before_value,''), "
        f"IFNULL(after_value,''), result, IFNULL(error_code,'') FROM tbl_admin_audit_log WHERE actor_user_id = {int(actor_id)} "
        "ORDER BY admin_audit_log_id"
    )
    return [line.split("\t") for line in out.splitlines() if line]


def test_14_admin_change_is_audited(base_url, s):
    target = s.user_ids["target"]
    resp = requests.patch(f"{_admin(base_url)}/v1/users/{target}/status", json={"status": "SUSPENDED"}, headers=_h(s, "admin"))
    assert_status(resp, 200)
    resp = requests.patch(f"{_admin(base_url)}/v1/users/{target}/status", json={"status": "NORMAL"}, headers=_h(s, "admin"))
    assert_status(resp, 200)

    rows = _audit_rows(s.user_ids["admin"])
    assert len(rows) == 2, rows
    action, method, path, tgt, detail, before, after, result, err = rows[0]
    assert action == "AdminUserController.updateUserStatus" and method == "PATCH", rows[0]
    assert path == f"/internal-api/v1/users/{target}/status", rows[0]
    assert json.loads(tgt) == {"userId": str(target)}, rows[0]
    assert json.loads(detail) == {"status": "SUSPENDED"}, rows[0]
    assert "name" not in json.loads(before), rows[0]  # 사용자 스냅샷에 이름 없음
    assert json.loads(before)["account_state"] == "NORMAL" and json.loads(after)["account_state"] == "SUSPENDED", rows[0]
    assert result == "SUCCESS" and err == "", rows[0]
    assert json.loads(rows[1][5])["account_state"] == "SUSPENDED" and json.loads(rows[1][6])["account_state"] == "NORMAL", rows[1]


def test_15_admin_failed_change_is_audited(base_url, s):
    """ADMIN 은 SUPER_ADMIN 의 상태를 바꿀 수 없다 → FAIL 기록 (오류 코드, 변경 후 값 없음)"""
    before = len(_audit_rows(s.user_ids["admin"]))
    resp = requests.patch(f"{_admin(base_url)}/v1/users/{s.user_ids['super']}/status", json={"status": "SUSPENDED"}, headers=_h(s, "admin"))
    assert resp.status_code == 403, resp.text
    rows = _audit_rows(s.user_ids["admin"])
    assert len(rows) == before + 1, rows
    assert rows[-1][7] == "FAIL" and rows[-1][8] == _code(resp) and rows[-1][6] == "", rows[-1]


def test_16_user_export_is_audited_and_reads_are_not(base_url, s):
    before = len(_audit_rows(s.user_ids["admin"]))
    resp = requests.get(f"{_admin(base_url)}/v1/users/export", params={"keyword": f"t763-{RUN}"}, headers=_h(s, "admin"))
    assert_status(resp, 200)
    assert_status(requests.get(f"{_admin(base_url)}/v1/users", headers=_h(s, "admin")), 200)
    rows = _audit_rows(s.user_ids["admin"])
    assert len(rows) == before + 1, rows
    assert rows[-1][0] == "AdminUserController.exportUsers" and rows[-1][1] == "GET", rows[-1]
    # 검색어는 개인정보일 수 있어 길이만 남긴다
    assert json.loads(rows[-1][4]) == {"keyword": f"***(len={len(f't763-{RUN}')})"}, rows[-1]


def test_16b_admin_request_succeeds_without_audit_table(base_url, s):
    """V011 DDL 이 아직 없는 DB 에 새 앱이 먼저 떠도 어드민 요청은 성공 (감사 저장만 실패)"""
    _sql("RENAME TABLE tbl_admin_audit_log TO tbl_admin_audit_log_763_bak")
    try:
        target = s.user_ids["target"]
        resp = requests.patch(f"{_admin(base_url)}/v1/users/{target}/status", json={"status": "SUSPENDED"}, headers=_h(s, "admin"))
        assert_status(resp, 200)
        assert get_data(resp)["accountState"] == "SUSPENDED", resp.text
        assert_status(requests.patch(f"{_admin(base_url)}/v1/users/{target}/status", json={"status": "NORMAL"}, headers=_h(s, "admin")), 200)
        assert_status(requests.get(f"{_admin(base_url)}/v1/users/export", headers=_h(s, "admin")), 200)
    finally:
        _sql("RENAME TABLE tbl_admin_audit_log_763_bak TO tbl_admin_audit_log")


# ===== 9. SUPER_ADMIN 예외: v1 공연 생성 = v2 =====

def test_17_super_admin_v1_create_event_like_v2(base_url, s):
    host = _new_host(base_url, headers=_h(s, "master"))
    assert_status(host, 200)
    host_id = get_data(host)["hostId"]
    body = {"hostId": host_id, "name": "t763공연", "startAt": START.strftime(FMT), "runTime": 90}
    assert_status(requests.post(f"{base_url}/v1/events", json=body, headers=_h(s, "super")), 200)
    resp = requests.post(f"{base_url}/v1/events", json=body, headers=_h(s, "user"))
    assert 400 <= resp.status_code < 500, resp.text
    # 없는 호스트면 SUPER_ADMIN 이어도 404 (존재 확인은 예외 없음)
    resp = requests.post(f"{base_url}/v1/events", json={**body, "hostId": 99999999}, headers=_h(s, "super"))
    assert resp.status_code == 404, resp.text


# ===== 4. 운영 프로필 (두 번째 서버) =====

@pytest.fixture(scope="module")
def prod_base_url():
    url = os.environ.get("E2E_PROD_PROFILE_BASE_URL")
    if not url:
        reason = "운영 프로필 서버 없음 (E2E_PROD_PROFILE_BASE_URL)"
        if os.environ.get("E2E_REQUIRE_PROD_PROFILE") == "1":
            pytest.fail(f"{reason} — E2E_REQUIRE_PROD_PROFILE=1")
        pytest.skip(reason)
    return url


def _preflight(base, origin):
    return requests.options(f"{base}/v1/auth/logout", headers={
        "Origin": origin, "Access-Control-Request-Method": "POST",
    })


def test_18_prod_profile_cors_has_no_localhost_or_staging(prod_base_url):
    for origin in ("http://localhost:3000", "http://localhost:5173", "https://staging.dudoong.com", EVIL_ORIGIN):
        resp = _preflight(prod_base_url, origin)
        assert resp.status_code == 403 and "Access-Control-Allow-Origin" not in resp.headers, (origin, resp.status_code, dict(resp.headers))
    for origin in ("https://dudoong.com", "https://internal-admin.dudoong.com"):
        resp = _preflight(prod_base_url, origin)
        assert resp.status_code == 200 and resp.headers.get("Access-Control-Allow-Origin") == origin, (origin, dict(resp.headers))
        assert resp.headers.get("Access-Control-Allow-Credentials") == "true"


def test_19_prod_profile_cookie_attributes_unchanged(prod_base_url):
    """운영 쿠키 속성은 기존 그대로: domain=.dudoong.com, SameSite=Strict, Secure (로그아웃 응답의 삭제 쿠키로 확인)"""
    resp = requests.post(f"{prod_base_url}/v1/auth/logout")
    assert_status(resp, 200)
    cookies = dict(_set_cookies(resp))
    for name in ("accessToken", "refreshToken"):
        attrs = cookies[name]
        assert attrs.get("domain") == ".dudoong.com" and attrs.get("samesite") == "Strict" and "secure" in attrs, attrs
        assert "httponly" not in attrs, attrs
