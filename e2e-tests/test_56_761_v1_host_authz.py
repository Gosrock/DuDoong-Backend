"""
v1 호스트·티켓 API 권한 / 응답 최소화 E2E 테스트 (#761).

시나리오: v1 호스트 상세는 활성 멤버만(slackUrl 은 매니저 이상, 멤버 개인정보 없음) → 초대 응답에 전화번호 없음 →
초대 대상 검색은 매니저 이상 → v1 역할 규칙(매니저는 GUEST 만 초대, 역할 변경은 마스터만, MASTER 지정 불가) →
v1 티켓·옵션 쓰기는 매니저 이상 → 공개 티켓 목록(준비중 404, 계좌 없음)·옵션 조회 권한 →
v1 공연 상세 본문 sanitize · 포스터/호스트 프로필 key 검증.

DB 직접 접근(slackUrl 설정)은 conftest 의 e2e_db fixture(환경변수 E2E_DB 등, #737)로 한다.
재실행해도 충돌하지 않도록 유저 이메일에 실행마다 다른 접미사를 붙인다.
"""
import base64
import json
import uuid
from datetime import datetime, timedelta

import pytest
import requests

from conftest import assert_status, get_data, issued_event_image_key, issued_host_image_key

RUN = uuid.uuid4().hex[:8]
FMT = "%Y.%m.%d %H:%M"
START = (datetime.now() + timedelta(days=30)).replace(hour=18, minute=0, second=0, microsecond=0)
END = START + timedelta(minutes=120)
PLACE = {"name": "롤링홀", "address": "서울 마포구 어울마당로 35", "latitude": 37.548369, "longitude": 126.920036}
SECTIONS = [{"title": "공연 소개", "content": "<p>#761</p>", "sortOrder": 0}]
PHONE = "010-7761-4321"
SLACK = "https://hooks.slack.com/services/e2e-761"
ACCOUNT = {"bank": "신한은행", "holder": "고스락", "number": "110-761-000000"}
PEOPLE = ["master", "manager", "guest", "outsider", "pending", "target"]
PRIVATE_MEMBER_FIELDS = ["email", "phoneNumber", "receiveMail", "marketingAgree", "createdAt"]


class State:
    tokens: dict = {}
    emails: dict = {}
    user_ids: dict = {}
    host_id: int = 0
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


def _h(s, who):
    return {"Authorization": f"Bearer {s.tokens[who]}"}


def _code(resp):
    return resp.json().get("code")


def _assert_error(resp, status, code):
    assert resp.status_code == status and _code(resp) == code, f"기대 {status} {code}, 실제 {resp.status_code}: {resp.text[:300]}"


def _login(base_url, email, name):
    resp = requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={"email": email, "name": name, "phoneNumber": PHONE, "profileImage": None, "marketingAgree": True},
    )
    assert_status(resp, 200)
    token = get_data(resp)["accessToken"]
    part = token.split(".")[1]
    part += "=" * (-len(part) % 4)
    return token, int(json.loads(base64.urlsafe_b64decode(part))["sub"])


def _v1(base_url, path):
    return f"{base_url}/v1{path}"


def _ev(base_url, event_id, path=""):
    return f"{base_url}/v2/events/{event_id}{path}"


def _new_event(base_url, s, key, open_event):
    """두둥티켓(계좌 있음) 1개짜리 공연. open_event 면 v2 오픈까지 한다"""
    resp = requests.post(
        f"{base_url}/v2/events",
        json={"hostId": s.host_id, "name": f"761공연{key}", "startAt": START.strftime(FMT), "endAt": END.strftime(FMT), "hasTicket": True},
        headers=_h(s, "manager"),
    )
    assert_status(resp, 200)
    event_id = get_data(resp)["eventId"]
    resp = requests.post(_ev(base_url, event_id, "/ticket-items"), json={
        "payType": "DUDOONG", "name": "두둥", "description": "두둥", "price": 3000, "supplyCount": 100, "account": ACCOUNT,
        "approvalRequired": True, "isQuantityPublic": True, "purchaseLimit": 4, "saleStartAt": None, "saleEndAt": None,
    }, headers=_h(s, "manager"))
    assert_status(resp, 200)
    s.events[key], s.tickets[key] = event_id, get_data(resp)["ticketItemId"]
    if open_event:
        key_img = get_data(requests.post(_ev(base_url, event_id, "/images"), json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "manager")))["key"]
        assert_status(requests.patch(_ev(base_url, event_id, "/basic"), json={"posterImageKey": key_img, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]}, headers=_h(s, "manager")), 200)
        assert_status(requests.put(_ev(base_url, event_id, "/sections"), json=SECTIONS, headers=_h(s, "manager")), 200)
        assert_status(requests.post(_ev(base_url, event_id, "/open"), headers=_h(s, "manager")), 200)
    return event_id


def _assert_no_private_member_fields(data, raw):
    for member in [data["masterUser"], *data["hostUsers"]]:
        assert {"userId", "userName", "role", "active"} <= member.keys(), member
        for field in PRIVATE_MEMBER_FIELDS:
            assert field not in member, f"멤버 응답에 {field} 노출: {member}"
    assert "7761" not in raw, f"응답에 전화번호 노출: {raw[:300]}"


def test_00_setup(base_url, s):
    for who in PEOPLE:
        s.emails[who] = f"e2e761-{who}-{RUN}@test.com"
        s.tokens[who], s.user_ids[who] = _login(base_url, s.emails[who], f"761{who}")
    resp = requests.post(f"{base_url}/v2/hosts", json={"name": f"761호스트{RUN}", "contacts": [{"type": "EMAIL", "value": "h@gosrock.band"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    s.host_id = get_data(resp)["hostId"]
    resp = requests.post(f"{base_url}/v2/hosts/{s.host_id}/members", json={"members": [
        {"email": s.emails["manager"], "role": "MANAGER"}, {"email": s.emails["guest"], "role": "GUEST"},
    ]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    # v1 초대 → 수락 전(초대 대기) 멤버
    assert_status(requests.post(_v1(base_url, f"/hosts/{s.host_id}/invite"), json={"email": s.emails["pending"], "role": "GUEST"}, headers=_h(s, "master")), 200)
    DB.query(f"UPDATE tbl_host SET slack_url = '{SLACK}' WHERE host_id = {s.host_id}")
    _new_event(base_url, s, "prep", open_event=False)
    _new_event(base_url, s, "open", open_event=True)


# ===== H-1 =====

def test_01_host_detail_members_only(base_url, s):
    url = _v1(base_url, f"/hosts/{s.host_id}")
    _assert_error(requests.get(url, headers=_h(s, "outsider")), 400, "HOST_400_2")
    _assert_error(requests.get(url, headers=_h(s, "pending")), 400, "HOST_400_6")


def test_02_host_detail_guest_minimal(base_url, s):
    resp = requests.get(_v1(base_url, f"/hosts/{s.host_id}"), headers=_h(s, "guest"))
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["slackUrl"] is None
    _assert_no_private_member_fields(data, resp.text)
    assert s.emails["manager"] not in resp.text
    # 초대 대기 멤버도 목록에는 보인다 (어드민 멤버 화면)
    pending = [m for m in data["hostUsers"] if m["userId"] == s.user_ids["pending"]]
    assert pending and pending[0]["active"] is False


def test_03_host_detail_manager_sees_slack(base_url, s):
    for who in ("manager", "master"):
        data = get_data(requests.get(_v1(base_url, f"/hosts/{s.host_id}"), headers=_h(s, who)))
        assert data["slackUrl"] == SLACK, who


# ===== H-2 =====

def test_04_invite_response_has_no_phone(base_url, s):
    resp = requests.post(_v1(base_url, f"/hosts/{s.host_id}/invite"), json={"email": s.emails["target"], "role": "GUEST"}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    _assert_no_private_member_fields(get_data(resp), resp.text)
    # 다음 테스트를 위해 초대 거절 (멤버에서 빠진다)
    assert_status(requests.post(_v1(base_url, f"/hosts/{s.host_id}/reject"), headers=_h(s, "target")), 200)


def test_05_invite_user_search_manager_only(base_url, s):
    url = _v1(base_url, f"/hosts/{s.host_id}/invite/users")
    _assert_error(requests.get(url, params={"email": s.emails["target"]}, headers=_h(s, "guest")), 400, "HOST_400_1")
    resp = requests.get(url, params={"email": s.emails["target"]}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["userId"] == s.user_ids["target"] and data["userName"] == "761target"
    assert "email" not in data and "phoneNumber" not in data


# ===== M-6 =====

def test_06_manager_can_invite_guest_only(base_url, s):
    url = _v1(base_url, f"/hosts/{s.host_id}/invite")
    _assert_error(requests.post(url, json={"email": s.emails["target"], "role": "MANAGER"}, headers=_h(s, "manager")), 400, "HOST_400_11")
    _assert_error(requests.post(url, json={"email": s.emails["target"], "role": "MASTER"}, headers=_h(s, "master")), 400, "HOST_400_10")
    assert_status(requests.post(url, json={"email": s.emails["target"], "role": "MANAGER"}, headers=_h(s, "master")), 200)


def test_07_role_change_master_only(base_url, s):
    url = _v1(base_url, f"/hosts/{s.host_id}/role")
    guest_id = s.user_ids["guest"]
    _assert_error(requests.patch(url, json={"userId": guest_id, "role": "MANAGER"}, headers=_h(s, "manager")), 400, "HOST_400_4")
    _assert_error(requests.patch(url, json={"userId": guest_id, "role": "MASTER"}, headers=_h(s, "master")), 400, "HOST_400_10")
    resp = requests.patch(url, json={"userId": guest_id, "role": "MANAGER"}, headers=_h(s, "master"))
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["masterUser"]["userId"] == s.user_ids["master"]
    assert [m["role"] for m in data["hostUsers"] if m["userId"] == guest_id] == ["매니저"]
    # 되돌리기
    assert_status(requests.patch(url, json={"userId": guest_id, "role": "GUEST"}, headers=_h(s, "master")), 200)


# ===== H-3 =====

def test_08_v1_ticket_writes_need_manager(base_url, s):
    event_id = s.events["prep"]
    base = _v1(base_url, f"/events/{event_id}")
    ticket = {"payType": "무료티켓", "name": "v1무료", "description": "설명", "price": 0, "supplyCount": 10,
              "approveType": "선착순", "isQuantityPublic": True, "purchaseLimit": 2}
    option = {"type": "Y/N", "name": "v1옵션", "description": "설명", "additionalPrice": 0}

    _assert_error(requests.post(f"{base}/ticketItems", json=ticket, headers=_h(s, "guest")), 400, "HOST_400_1")
    _assert_error(requests.post(f"{base}/ticketOptions", json=option, headers=_h(s, "guest")), 400, "HOST_400_1")
    resp = requests.post(f"{base}/ticketItems", json=ticket, headers=_h(s, "manager"))
    assert_status(resp, 200)
    ticket_id = get_data(resp)["ticketItemId"]
    resp = requests.post(f"{base}/ticketOptions", json=option, headers=_h(s, "manager"))
    assert_status(resp, 200)
    group = {"optionGroupId": get_data(resp)["optionGroupId"]}

    for path in (f"/ticketItems/{ticket_id}/option", f"/ticketItems/{ticket_id}/option/cancel"):
        _assert_error(requests.patch(f"{base}{path}", json=group, headers=_h(s, "guest")), 400, "HOST_400_1")
        assert_status(requests.patch(f"{base}{path}", json=group, headers=_h(s, "manager")), 200)
    for path in (f"/ticketOptions/{group['optionGroupId']}", f"/ticketItems/{ticket_id}"):
        _assert_error(requests.patch(f"{base}{path}", headers=_h(s, "guest")), 400, "HOST_400_1")
        assert_status(requests.patch(f"{base}{path}", headers=_h(s, "manager")), 200)


# ===== M-1 =====

def test_09_public_ticket_items(base_url, s):
    # 준비중 공연은 404
    _assert_error(requests.get(_v1(base_url, f"/events/{s.events['prep']}/ticketItems")), 404, "Event_404_1")
    # 공개 공연: 비로그인 조회 가능, 계좌 없음
    resp = requests.get(_v1(base_url, f"/events/{s.events['open']}/ticketItems"))
    assert_status(resp, 200)
    items = get_data(resp)["ticketItems"]
    assert [i["ticketItemId"] for i in items] == [s.tickets["open"]]
    assert items[0]["accountInfo"] is None and ACCOUNT["number"] not in resp.text
    # 호스트 관리용 목록에는 계좌가 있다
    admin = get_data(requests.get(_v1(base_url, f"/events/{s.events['open']}/ticketItems/admin"), headers=_h(s, "guest")))
    assert admin["ticketItems"][0]["accountInfo"]["accountNumber"] == ACCOUNT["number"]


def test_10_option_reads(base_url, s):
    event_id = s.events["open"]
    for path in ("/ticketItems/appliedOptionGroups", "/ticketOptions"):
        url = _v1(base_url, f"/events/{event_id}{path}")
        _assert_error(requests.get(url, headers=_h(s, "outsider")), 400, "HOST_400_2")
        assert_status(requests.get(url, headers=_h(s, "guest")), 200)
    # 구매 플로우의 티켓 옵션 조회는 일반 사용자도 가능
    assert_status(requests.get(_v1(base_url, f"/events/{event_id}/ticketItems/{s.tickets['open']}/options"), headers=_h(s, "outsider")), 200)


# ===== X-1 / X-3 =====

def test_11_event_detail_sanitize_and_poster_key(base_url, s):
    event_id = s.events["prep"]
    url = _v1(base_url, f"/events/{event_id}/details")
    poster = issued_event_image_key(base_url, _h(s, "manager"), event_id)
    content = ('<h2>공지</h2><p>본문<script>alert(1)</script><img src="https://cdn.example.com/a.png" onerror="alert(2)">'
               '<span style="color: #ff0000">빨강</span><span style="position:fixed">덮기</span><del>취소</del></p><hr>')

    for bad in ("https://evil.example.com/a.png", f"{poster}?x", poster.replace(f"/event/{event_id}/", f"/event/{s.events['open']}/")):
        _assert_error(requests.patch(url, json={"posterImageKey": bad, "content": "본문"}, headers=_h(s, "manager")), 400, "Event_400_22")
    assert_status(requests.patch(url, json={"posterImageKey": poster, "content": content}, headers=_h(s, "manager")), 200)
    # 지금 저장된 key 를 그대로 다시 보내는 것은 허용
    resp = requests.patch(url, json={"posterImageKey": poster, "content": content}, headers=_h(s, "manager"))
    assert_status(resp, 200)

    saved = DB.query(f"SELECT content FROM tbl_event WHERE event_id = {event_id}").strip()
    for bad in ("<script", "alert(1)", "onerror", "position:fixed"):
        assert bad not in saved, f"sanitize 누락({bad}): {saved}"
    for kept in ("<h2>공지</h2>", '<span style="color: #ff0000">빨강</span>', "<del>취소</del>", "<hr>", "덮기"):
        assert kept in saved, f"서식 손실({kept}): {saved}"


def test_12_host_profile_image_key(base_url, s):
    url = _v1(base_url, f"/hosts/{s.host_id}/profile")
    body = {"introduce": "761", "contactNumber": "010-1111-2222", "contactEmail": "h@gosrock.band"}
    for bad in ("https://evil.example.com/a.png", "e2e/host/profile.png"):
        _assert_error(requests.patch(url, json={**body, "profileImageKey": bad}, headers=_h(s, "manager")), 400, "HOST_400_17")
    key = issued_host_image_key(base_url, _h(s, "manager"), s.host_id)
    assert_status(requests.patch(url, json={**body, "profileImageKey": key}, headers=_h(s, "manager")), 200)
    assert_status(requests.patch(url, json={**body, "profileImageKey": key}, headers=_h(s, "manager")), 200)
