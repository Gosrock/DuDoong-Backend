"""
v2 호스트 / 멤버 API E2E 테스트 (#704).

시나리오: 호스트 생성(연락처 여러 개) → 공개 홈(비로그인 포함, 멤버목록/slackUrl 미노출) → 수정
→ 멤버 일괄 추가 → 매니저 권한 경계(403) → 역할 변경 / 마스터 양도 → 멤버 삭제
→ 팔로우 멱등 → 호스트 공연 리스트 공개/멤버 차이 → v1 호환.

재실행해도 충돌하지 않도록 유저 이메일에 실행마다 다른 접미사를 붙인다.
"""
import uuid
from datetime import datetime, timedelta

import pytest
import requests

from conftest import assert_status, get_data, issued_event_image_key, issued_host_image_key

RUN = uuid.uuid4().hex[:8]


class V2HostState:
    host_id: int = 0
    tokens: dict = {}
    user_ids: dict = {}
    emails: dict = {}
    preparing_event_id: int = 0
    open_event_id: int = 0


@pytest.fixture(scope="module")
def s():
    return V2HostState()


def _h(s, who):
    return {"Authorization": f"Bearer {s.tokens[who]}"}


def _login(base_url, email, name):
    resp = requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={
            "email": email,
            "name": name,
            "phoneNumber": "010-0000-0000",
            "profileImage": None,
            "marketingAgree": False,
        },
    )
    assert_status(resp, 200)
    token = get_data(resp)["accessToken"]
    me = requests.get(f"{base_url}/v1/users/me", headers={"Authorization": f"Bearer {token}"})
    assert_status(me, 200)
    me_data = get_data(me)
    return token, me_data.get("userId") or me_data.get("id")


def _members(base_url, s, who="master"):
    resp = requests.get(f"{base_url}/v2/hosts/{s.host_id}/members", headers=_h(s, who))
    assert_status(resp, 200)
    return {m["userId"]: m["role"] for m in get_data(resp)}


def _add_members(base_url, s, who, members):
    return requests.post(
        f"{base_url}/v2/hosts/{s.host_id}/members",
        json={"members": [{"email": e, "role": r} for e, r in members]},
        headers=_h(s, who),
    )


def _future(days):
    return (datetime.now() + timedelta(days=days)).strftime("%Y.%m.%d %H:%M")


def test_01_setup_users(base_url, s):
    for who, name in [
        ("master", "브이투마스터"),
        ("manager", "브이투매니저"),
        ("guest", "브이투일반"),
        ("guest2", "브이투일반둘"),
        ("outsider", "브이투외부"),
    ]:
        email = f"v2host-{who}-{RUN}@dudoong.com"
        token, user_id = _login(base_url, email, name)
        assert user_id, f"{who} userId 조회 실패"
        s.tokens[who], s.user_ids[who], s.emails[who] = token, user_id, email


def test_02_create_host_with_contacts(base_url, s):
    resp = requests.post(
        f"{base_url}/v2/hosts",
        json={
            "name": f"브이투{RUN[:4]}",
            "introduce": "v2 E2E 호스트",
            "contacts": [
                {"type": "INSTAGRAM", "value": "@gosrock"},
                {"type": "EMAIL", "value": "v2host@dudoong.com"},
                {"type": "PHONE", "value": "010-1234-5678"},
                {"type": "OPEN_CHAT", "value": "https://open.kakao.com/o/test"},
            ],
        },
        headers=_h(s, "master"),
    )
    assert_status(resp, 200)
    s.host_id = get_data(resp)["hostId"]
    assert s.host_id


def test_03_create_host_validation(base_url, s):
    for body in [
        {"name": "연락처없음", "contacts": []},
        {"name": "가" * 16, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]},
        {"name": "전화너무김", "contacts": [{"type": "PHONE", "value": "0" * 16}]},
        {"name": "없는유형", "contacts": [{"type": "FAX", "value": "1"}]},
    ]:
        resp = requests.post(f"{base_url}/v2/hosts", json=body, headers=_h(s, "master"))
        assert_status(resp, 400)


def test_04_public_home_anonymous(base_url, s):
    resp = requests.get(f"{base_url}/v2/hosts/{s.host_id}")
    assert_status(resp, 200)
    data = get_data(resp)
    assert [c["type"] for c in data["contacts"]] == ["INSTAGRAM", "EMAIL", "PHONE", "OPEN_CHAT"]
    assert data["memberCount"] == 1
    assert data["isFollowing"] is False
    assert data["myRole"] is None
    assert data["createdAt"]
    for hidden in ("hostUsers", "masterUser", "members", "slackUrl", "partner"):
        assert hidden not in data, f"공개 홈에 {hidden} 노출: {data}"


def test_05_public_home_master_role(base_url, s):
    data = get_data(requests.get(f"{base_url}/v2/hosts/{s.host_id}", headers=_h(s, "master")))
    assert data["myRole"] == "MASTER"


def test_06_update_host_partial(base_url, s):
    # 커버 key 는 H-15 가 이 호스트에 발급한 것만 허용
    upload = requests.post(
        f"{base_url}/v2/hosts/{s.host_id}/images",
        json={"purpose": "COVER", "extension": "PNG"},
        headers=_h(s, "master"),
    )
    assert_status(upload, 200)
    cover_key = get_data(upload)["key"]
    resp = requests.patch(
        f"{base_url}/v2/hosts/{s.host_id}",
        json={"introduce": "수정된 소개", "coverImageKey": cover_key},
        headers=_h(s, "master"),
    )
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["introduce"] == "수정된 소개"
    assert data["coverImageUrl"].endswith(cover_key)
    assert data["name"].startswith("브이투")  # null 필드는 유지
    assert len(data["contacts"]) == 4


def test_07_add_members_bulk(base_url, s):
    resp = _add_members(base_url, s, "master", [(s.emails["manager"], "MANAGER"), (s.emails["guest"], "GUEST")])
    assert_status(resp, 200)
    roles = _members(base_url, s)
    assert roles[s.user_ids["manager"]] == "MANAGER"
    assert roles[s.user_ids["guest"]] == "GUEST"
    assert list(roles.values())[0] == "MASTER"


def test_08_add_members_errors_rollback(base_url, s):
    unknown = f"nobody-{RUN}@dudoong.com"
    resp = _add_members(base_url, s, "master", [(s.emails["guest2"], "GUEST"), (unknown, "GUEST")])
    assert_status(resp, 400)
    assert resp.json()["code"] == "HOST_400_13"
    assert unknown in resp.json()["reason"]
    assert s.user_ids["guest2"] not in _members(base_url, s)  # 전체 롤백

    resp = _add_members(base_url, s, "master", [(s.emails["guest"], "GUEST")])
    assert_status(resp, 400)
    assert resp.json()["code"] == "HOST_400_15"

    resp = _add_members(base_url, s, "master", [(s.emails["guest2"], "MASTER")])
    assert_status(resp, 400)
    assert resp.json()["code"] == "HOST_400_10"


def test_08b_add_member_ambiguous_email(base_url, s, e2e_db):
    """같은 이메일의 정상 계정이 2개 이상이면 누구인지 특정할 수 없어 400 (HOST_400_18)"""
    email = f"v2host-dup-{RUN}@dudoong.com"
    for i in range(2):
        e2e_db.query(
            "INSERT INTO tbl_user (created_at, updated_at, account_role, account_state, last_login_at, "
            "marketing_agree, oid, provider, email, name, receive_mail) VALUES "
            f"(NOW(), NOW(), 'USER', 'NORMAL', NOW(), b'0', 'v2host-dup-{RUN}-{i}', 'KAKAO', '{email}', '중복{i}', b'1')"
        )
    resp = _add_members(base_url, s, "master", [(email, "GUEST")])
    assert_status(resp, 400)
    assert resp.json()["code"] == "HOST_400_18"
    assert email in resp.json()["reason"]


def test_09_manager_permission_boundary(base_url, s):
    # 매니저가 MANAGER 추가 → 403
    resp = _add_members(base_url, s, "manager", [(s.emails["guest2"], "MANAGER")])
    assert_status(resp, 403)
    assert resp.json()["code"] == "HOST_400_11"
    # 매니저는 GUEST 추가 가능
    assert_status(_add_members(base_url, s, "manager", [(s.emails["guest2"], "GUEST")]), 200)
    # 매니저는 역할 변경 불가 (MS)
    resp = requests.patch(
        f"{base_url}/v2/hosts/{s.host_id}/members/{s.user_ids['guest']}/role",
        json={"role": "MANAGER"},
        headers=_h(s, "manager"),
    )
    assert_status(resp, 403)
    # 일반 멤버는 수정/추가 불가, 멤버 목록은 가능
    assert_status(requests.patch(f"{base_url}/v2/hosts/{s.host_id}", json={"introduce": "x"}, headers=_h(s, "guest")), 403)
    assert_status(_add_members(base_url, s, "guest", [(s.emails["outsider"], "GUEST")]), 403)
    _members(base_url, s, "guest")
    # 비멤버는 멤버 목록 403, 비로그인은 401
    assert_status(requests.get(f"{base_url}/v2/hosts/{s.host_id}/members", headers=_h(s, "outsider")), 403)
    assert_status(requests.get(f"{base_url}/v2/hosts/{s.host_id}/members"), 401)


def test_10_change_role(base_url, s):
    url = f"{base_url}/v2/hosts/{s.host_id}/members/{s.user_ids['guest']}/role"
    assert_status(requests.patch(url, json={"role": "MANAGER"}, headers=_h(s, "master")), 200)
    assert _members(base_url, s)[s.user_ids["guest"]] == "MANAGER"
    assert_status(requests.patch(url, json={"role": "GUEST"}, headers=_h(s, "master")), 200)
    assert _members(base_url, s)[s.user_ids["guest"]] == "GUEST"

    master_url = f"{base_url}/v2/hosts/{s.host_id}/members/{s.user_ids['master']}/role"
    resp = requests.patch(master_url, json={"role": "GUEST"}, headers=_h(s, "master"))
    assert_status(resp, 400)
    assert resp.json()["code"] == "HOST_400_5"


def test_11_remove_member_rules(base_url, s):
    base = f"{base_url}/v2/hosts/{s.host_id}/members"
    # 매니저는 마스터 삭제 불가(400), 본인(매니저) 삭제 불가(403)
    resp = requests.delete(f"{base}/{s.user_ids['master']}", headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert resp.json()["code"] == "HOST_400_12"
    assert_status(requests.delete(f"{base}/{s.user_ids['manager']}", headers=_h(s, "manager")), 403)
    # 매니저는 일반 멤버 삭제 가능
    assert_status(requests.delete(f"{base}/{s.user_ids['guest2']}", headers=_h(s, "manager")), 200)
    assert s.user_ids["guest2"] not in _members(base_url, s)


def test_12_v1_list_excludes_removed_member(base_url, s):
    resp = requests.get(f"{base_url}/v1/hosts", headers=_h(s, "guest2"))
    assert_status(resp, 200)
    host_ids = [h["hostId"] for h in get_data(resp)["content"]]
    assert s.host_id not in host_ids


def test_13_follow_idempotent(base_url, s):
    url = f"{base_url}/v2/hosts/{s.host_id}/follow"
    for _ in range(2):
        resp = requests.put(url, headers=_h(s, "outsider"))
        assert_status(resp, 200)
        assert get_data(resp)["isFollowing"] is True
        assert get_data(resp)["followerCount"] == 1
    home = get_data(requests.get(f"{base_url}/v2/hosts/{s.host_id}", headers=_h(s, "outsider")))
    assert home["isFollowing"] is True and home["followerCount"] == 1
    assert get_data(requests.get(f"{base_url}/v2/hosts/{s.host_id}"))["isFollowing"] is False
    for _ in range(2):
        resp = requests.delete(url, headers=_h(s, "outsider"))
        assert_status(resp, 200)
        assert get_data(resp)["followerCount"] == 0
    assert_status(requests.put(url), 401)


def test_14_prepare_events(base_url, s):
    headers = _h(s, "master")
    # 준비중 공연
    resp = requests.post(
        f"{base_url}/v1/events",
        json={"hostId": s.host_id, "name": "v2준비중공연", "startAt": _future(20), "runTime": 60},
        headers=headers,
    )
    assert resp.status_code in (200, 201), resp.text[:300]
    s.preparing_event_id = get_data(resp)["eventId"]

    # 오픈 공연
    resp = requests.post(
        f"{base_url}/v1/events",
        json={"hostId": s.host_id, "name": "v2오픈공연", "startAt": _future(30), "runTime": 60},
        headers=headers,
    )
    assert resp.status_code in (200, 201), resp.text[:300]
    event_id = get_data(resp)["eventId"]
    assert_status(requests.patch(
        f"{base_url}/v1/events/{event_id}/basic",
        json={
            "name": "v2오픈공연",
            "startAt": _future(30),
            "runTime": 60,
            "placeName": "테스트공연장",
            "placeAddress": "서울 마포구 어울마당로 35",
            "longitude": 126.920036,
            "latitude": 37.548369,
        },
        headers=headers,
    ), 200)
    assert_status(requests.patch(
        f"{base_url}/v1/events/{event_id}/details",
        json={"posterImageKey": issued_event_image_key(base_url, headers, event_id), "content": "v2 E2E"},
        headers=headers,
    ), 200)
    resp = requests.post(
        f"{base_url}/v1/events/{event_id}/ticketItems",
        json={
            "payType": "무료티켓",
            "name": "v2무료티켓",
            "description": "v2",
            "price": 0,
            "supplyCount": 10,
            "approveType": "선착순",
            "isQuantityPublic": True,
            "purchaseLimit": 1,
        },
        headers=headers,
    )
    assert resp.status_code in (200, 201), resp.text[:300]
    assert_status(requests.patch(f"{base_url}/v1/events/{event_id}/open", headers=headers), 200)
    s.open_event_id = event_id


def test_15_host_events_public_vs_member(base_url, s):
    url = f"{base_url}/v2/hosts/{s.host_id}/events"

    public = get_data(requests.get(url))
    public_ids = [e["eventId"] for e in public["content"]]
    assert public_ids == [s.open_event_id]
    assert public["content"][0]["displayStatus"] == "UPCOMING"
    assert public["content"][0]["status"] == "OPEN"
    assert public["totalElements"] == 1

    outsider = get_data(requests.get(url, headers=_h(s, "outsider")))
    assert [e["eventId"] for e in outsider["content"]] == [s.open_event_id]

    member = get_data(requests.get(url, headers=_h(s, "guest")))
    by_id = {e["eventId"]: e["displayStatus"] for e in member["content"]}
    assert by_id == {s.open_event_id: "UPCOMING", s.preparing_event_id: "PREPARING"}


def test_16_my_hosts(base_url, s):
    resp = requests.get(f"{base_url}/v2/me/hosts", params={"keyword": "브이투"}, headers=_h(s, "guest"))
    assert_status(resp, 200)
    data = get_data(resp)
    mine = [h for h in data["content"] if h["hostId"] == s.host_id]
    assert len(mine) == 1
    assert mine[0]["myRole"] == "GUEST"
    assert mine[0]["eventCount"] == 2
    assert data["totalElements"] is not None

    # 삭제된 멤버의 목록에는 없다
    removed = get_data(requests.get(f"{base_url}/v2/me/hosts", headers=_h(s, "guest2")))
    assert s.host_id not in [h["hostId"] for h in removed["content"]]


def test_17_image_upload_url(base_url, s):
    resp = requests.post(
        f"{base_url}/v2/hosts/{s.host_id}/images",
        json={"purpose": "COVER", "extension": "PNG"},
        headers=_h(s, "manager"),
    )
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["purpose"] == "COVER"
    assert f"/host/{s.host_id}/" in data["key"]
    assert data["presignedUrl"]
    assert_status(requests.post(
        f"{base_url}/v2/hosts/{s.host_id}/images",
        json={"purpose": "PROFILE", "extension": "PNG"},
        headers=_h(s, "guest"),
    ), 403)


def test_18_master_transfer(base_url, s):
    url = f"{base_url}/v2/hosts/{s.host_id}/transfer-master"
    assert_status(requests.post(url, json={"userId": s.user_ids["guest"]}, headers=_h(s, "manager")), 403)
    # 비멤버 대상은 권한 문제가 아니라 대상 없음 → 404
    resp = requests.post(url, json={"userId": s.user_ids["outsider"]}, headers=_h(s, "master"))
    assert_status(resp, 404)
    assert resp.json()["code"] == "HOST_404_2"

    resp = requests.post(url, json={"userId": s.user_ids["manager"]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    roles = {m["userId"]: m["role"] for m in get_data(resp)}
    assert roles[s.user_ids["manager"]] == "MASTER"
    assert roles[s.user_ids["master"]] == "MANAGER"


def test_19_v1_compat_contacts_and_members(base_url, s):
    resp = requests.get(f"{base_url}/v1/hosts/{s.host_id}", headers=_h(s, "master"))
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["contactEmail"] == "v2host@dudoong.com"
    assert data["contactNumber"] == "010-1234-5678"
    assert data["masterUser"]["userId"] == s.user_ids["manager"]

    # v2 로 연락처를 바꾸면 v1 필드도 따라간다. PHONE 이 빠지면 v1 contactNumber 는 기존 값 유지
    resp = requests.patch(
        f"{base_url}/v2/hosts/{s.host_id}",
        json={"contacts": [{"type": "EMAIL", "value": "changed@dudoong.com"}]},
        headers=_h(s, "manager"),
    )
    assert_status(resp, 200)
    data = get_data(requests.get(f"{base_url}/v1/hosts/{s.host_id}", headers=_h(s, "master")))
    assert data["contactEmail"] == "changed@dudoong.com"
    assert data["contactNumber"] == "010-1234-5678"


def test_20_v1_profile_patch_syncs_v2_contacts(base_url, s):
    """v1 PATCH profile 로 바뀐 전화번호/이메일이 v2 연락처(첫 EMAIL/PHONE, 없으면 추가)에 반영된다"""
    resp = requests.patch(
        f"{base_url}/v1/hosts/{s.host_id}/profile",
        json={
            "profileImageKey": issued_host_image_key(base_url, _h(s, "manager"), s.host_id),
            "introduce": "v1 소개",
            "contactNumber": "010-9999-8888",
            "contactEmail": "v1@dudoong.com",
        },
        headers=_h(s, "manager"),
    )
    assert_status(resp, 200)
    assert "contacts" not in get_data(resp)  # v1 응답 형식 불변
    contacts = get_data(requests.get(f"{base_url}/v2/hosts/{s.host_id}"))["contacts"]
    assert contacts == [
        {"type": "EMAIL", "value": "v1@dudoong.com"},
        {"type": "PHONE", "value": "010-9999-8888"},
    ]


def test_21_image_key_must_be_host_prefixed(base_url, s):
    for key in ["https://evil.example.com/a.png", "e2e/host/cover.png"]:
        resp = requests.patch(f"{base_url}/v2/hosts/{s.host_id}", json={"coverImageKey": key}, headers=_h(s, "manager"))
        assert_status(resp, 400)
        assert resp.json()["code"] == "HOST_400_17"
