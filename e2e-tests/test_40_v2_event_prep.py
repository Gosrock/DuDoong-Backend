"""
v2 공연 준비 API E2E 테스트 (#705).

시나리오: 호스트 생성 → 태그 목록(비로그인) → 간편 공연 만들기(검증 실패 포함) → 체크리스트 단계별 확인
(기본정보·문의처·태그 → 섹션 → 티켓[v1 API]) → v2 등록 → 공개 섹션 조회(비로그인)
→ 등록 후 기본정보 수정 가능 / hasTicket 변경 불가 → v1 상세 호환(runTime·endAt·content) / v1 details → v2 섹션
→ 티켓 없는 공연 v2 등록 (v1 open 은 여전히 티켓 필요) → 삭제 규칙 → 내 공연 목록 / 권한 경계.

재실행해도 충돌하지 않도록 유저 이메일에 실행마다 다른 접미사를 붙인다. DB 직접 접근은 하지 않는다.
"""
import uuid
from datetime import datetime, timedelta

import pytest
import requests

from conftest import assert_status, get_data, issued_event_image_key

RUN = uuid.uuid4().hex[:8]
FMT = "%Y.%m.%d %H:%M"
START = (datetime.now() + timedelta(days=30)).replace(hour=18, minute=0, second=0, microsecond=0)
END = START + timedelta(minutes=200)
PLACE = {"name": "롤링홀", "address": "서울 마포구 어울마당로 35", "latitude": 37.548369, "longitude": 126.920036}
SECTIONS = [
    {"title": "공연 소개", "content": "<p>v2 소개</p>", "sortOrder": 0},
    {"title": "예매안내", "content": "예매 안내", "sortOrder": 1},
    {"title": "세트리스트", "content": "", "sortOrder": 2},
    {"title": "유의사항", "content": None, "sortOrder": 3},
]


class V2EventState:
    host_id: int = 0
    other_host_id: int = 0
    tokens: dict = {}
    user_ids: dict = {}
    emails: dict = {}
    tag_ids: dict = {}
    event_id: int = 0
    no_ticket_event_id: int = 0


@pytest.fixture(scope="module")
def s():
    return V2EventState()


def _f(dt):
    return dt.strftime(FMT)


def _h(s, who):
    return {"Authorization": f"Bearer {s.tokens[who]}"}


def _login(base_url, email, name):
    resp = requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={"email": email, "name": name, "phoneNumber": "010-0000-0000", "profileImage": None, "marketingAgree": False},
    )
    assert_status(resp, 200)
    token = get_data(resp)["accessToken"]
    me = get_data(requests.get(f"{base_url}/v1/users/me", headers={"Authorization": f"Bearer {token}"}))
    return token, me.get("userId") or me.get("id")


def _create_event(base_url, s, who="manager", host_id=None, **overrides):
    body = {"hostId": host_id or s.host_id, "name": "v2공연", "startAt": _f(START), "endAt": _f(END), "hasTicket": True}
    body.update(overrides)
    return requests.post(f"{base_url}/v2/events", json=body, headers=_h(s, who))


def _checklist(base_url, s, event_id, who="guest"):
    resp = requests.get(f"{base_url}/v2/events/{event_id}/checklist", headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)


def _patch_basic(base_url, s, event_id, body, who="manager"):
    return requests.patch(f"{base_url}/v2/events/{event_id}/basic", json=body, headers=_h(s, who))


def _poster_key(base_url, s, event_id):
    resp = requests.post(f"{base_url}/v2/events/{event_id}/images", json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    return get_data(resp)["key"]


def _fill(base_url, s, event_id):
    body = {"posterImageKey": _poster_key(base_url, s, event_id), "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]}
    assert_status(_patch_basic(base_url, s, event_id, body), 200)
    assert_status(requests.put(f"{base_url}/v2/events/{event_id}/sections", json={"sections": SECTIONS}, headers=_h(s, "manager")), 200)


def _v1_free_ticket(base_url, s, event_id):
    resp = requests.post(
        f"{base_url}/v1/events/{event_id}/ticketItems",
        json={
            "payType": "무료티켓",
            "name": "v2무료",
            "description": "v2",
            "price": 0,
            "supplyCount": 10,
            "approveType": "선착순",
            "isQuantityPublic": True,
            "purchaseLimit": 1,
        },
        headers=_h(s, "master"),
    )
    assert resp.status_code in (200, 201), resp.text[:300]


def test_01_setup(base_url, s):
    for who, name in [("master", "공연마스터"), ("manager", "공연매니저"), ("guest", "공연일반"), ("outsider", "공연외부"), ("other", "다른호스트")]:
        email = f"v2event-{who}-{RUN}@dudoong.com"
        token, user_id = _login(base_url, email, name)
        assert user_id
        s.tokens[who], s.user_ids[who], s.emails[who] = token, user_id, email

    for who, attr, host_name in [("master", "host_id", f"공연준비{RUN[:4]}"), ("other", "other_host_id", f"남의호스트{RUN[:3]}")]:
        resp = requests.post(
            f"{base_url}/v2/hosts",
            json={"name": host_name, "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]},
            headers=_h(s, who),
        )
        assert_status(resp, 200)
        setattr(s, attr, get_data(resp)["hostId"])

    resp = requests.post(
        f"{base_url}/v2/hosts/{s.host_id}/members",
        json={"members": [{"email": s.emails["manager"], "role": "MANAGER"}, {"email": s.emails["guest"], "role": "GUEST"}]},
        headers=_h(s, "master"),
    )
    assert_status(resp, 200)


def test_02_tags_public(base_url, s):
    resp = requests.get(f"{base_url}/v2/tags")
    assert_status(resp, 200)
    groups = get_data(resp)
    assert [g["category"] for g in groups] == ["EVENT_TYPE", "GENRE", "AREA", "TEAM"]
    by_name = {(g["category"], t["name"]): t["tagId"] for g in groups for t in g["tags"]}
    for key in [("EVENT_TYPE", "정기공연"), ("GENRE", "락밴드"), ("AREA", "홍대"), ("TEAM", "대학밴드")]:
        assert key in by_name, f"시드 태그 없음: {key}"
    s.tag_ids = by_name


def test_03_create_event_validation_and_permission(base_url, s):
    for overrides, code in [({"endAt": _f(START)}, "Event_400_2"), ({"endAt": _f(START - timedelta(minutes=1))}, "Event_400_2")]:
        resp = _create_event(base_url, s, **overrides)
        assert_status(resp, 400)
        assert resp.json()["code"] == code
    assert_status(_create_event(base_url, s, name="가" * 26), 400)
    assert_status(_create_event(base_url, s, hasTicket=None), 400)
    assert_status(_create_event(base_url, s, who="guest"), 403)
    assert_status(_create_event(base_url, s, who="outsider"), 403)
    assert_status(requests.post(f"{base_url}/v2/events", json={"hostId": s.host_id}), 401)


def test_04_create_event(base_url, s):
    resp = _create_event(base_url, s, name="v2정기공연")
    assert_status(resp, 200)
    s.event_id = get_data(resp)["eventId"]

    manage = get_data(requests.get(f"{base_url}/v2/events/{s.event_id}/manage", headers=_h(s, "guest")))
    assert manage["status"] == "PREPARING"
    assert manage["displayStatus"] == "PREPARING"
    assert manage["startAt"] == _f(START)
    assert manage["endAt"] == _f(END)
    assert manage["runTime"] == 200
    assert manage["hasTicket"] is True
    assert manage["myRole"] == "GUEST"
    assert manage["contacts"] == [] and manage["tags"] == []

    c = _checklist(base_url, s, s.event_id)
    assert c == {"isBasicFilled": False, "isDetailFilled": False, "hasValidTicket": False, "ticketRequired": True, "canOpen": False}
    resp = requests.post(f"{base_url}/v2/events/{s.event_id}/open", headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert resp.json()["code"] == "Event_400_7"


def test_05_basic_info_contacts_tags(base_url, s):
    resp = requests.post(
        f"{base_url}/v2/events/{s.event_id}/images", json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "manager")
    )
    assert_status(resp, 200)
    poster_key = get_data(resp)["key"]
    assert f"/event/{s.event_id}/" in poster_key

    tag_ids = [s.tag_ids[("AREA", "홍대")], s.tag_ids[("EVENT_TYPE", "정기공연")], s.tag_ids[("GENRE", "락밴드")]]
    # 포스터 없이 장소·문의처만 있으면 기본 정보 미충족
    resp = _patch_basic(base_url, s, s.event_id, {"place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]})
    assert_status(resp, 200)
    assert _checklist(base_url, s, s.event_id)["isBasicFilled"] is False

    resp = _patch_basic(
        base_url,
        s,
        s.event_id,
        {
            "posterImageKey": poster_key,
            "place": PLACE,
            "contacts": [{"type": "INSTAGRAM", "value": "@gosrock"}, {"type": "OPEN_CHAT", "value": "https://open.kakao.com/o/x"}],
            "tagIds": tag_ids,
        },
    )
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["posterImageKey"] == poster_key
    assert data["place"]["name"] == "롤링홀"
    assert [c["type"] for c in data["contacts"]] == ["INSTAGRAM", "OPEN_CHAT"]
    assert [t["category"] for t in data["tags"]] == ["EVENT_TYPE", "GENRE", "AREA"]

    c = _checklist(base_url, s, s.event_id)
    assert c["isBasicFilled"] is True and c["isDetailFilled"] is False and c["canOpen"] is False

    # 잘못된 값
    for body, code in [
        ({"posterImageKey": "https://evil.example.com/a.png"}, "Event_400_22"),
        ({"tagIds": [987654321]}, "Event_400_23"),
        ({"endAt": _f(START - timedelta(hours=1))}, "Event_400_2"),
    ]:
        resp = _patch_basic(base_url, s, s.event_id, body)
        assert_status(resp, 400)
        assert resp.json()["code"] == code
    assert_status(_patch_basic(base_url, s, s.event_id, {"name": "가" * 26}), 400)
    assert_status(_patch_basic(base_url, s, s.event_id, {"contacts": [{"type": "ETC", "value": str(i)} for i in range(11)]}), 400)
    # 태그는 중복 제거 후 개수 검증
    resp = _patch_basic(base_url, s, s.event_id, {"tagIds": tag_ids * 4})
    assert_status(resp, 200)
    assert len(get_data(resp)["tags"]) == 3


def test_06_sections(base_url, s):
    url = f"{base_url}/v2/events/{s.event_id}/sections"
    # HTML 은 서버에서 sanitize
    dirty = [{"title": "공연 소개", "content": '<p>x<script>alert(1)</script></p><img src="https://a.com/a.png" onerror="alert(2)">', "sortOrder": 0}]
    saved = get_data(requests.put(url, json={"sections": dirty}, headers=_h(s, "manager")))[0]
    assert "<script" not in saved["content"] and "onerror" not in saved["content"]
    assert '<img src="https://a.com/a.png">' in saved["content"]

    resp = requests.put(url, json={"sections": SECTIONS}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert [x["title"] for x in get_data(resp)] == ["공연 소개", "예매안내", "세트리스트", "유의사항"]
    assert {x["contentFormat"] for x in get_data(resp)} == {"HTML"}

    # 준비중 공연 섹션: 비로그인/비멤버 404, 멤버 200
    assert_status(requests.get(url), 404)
    assert_status(requests.get(url, headers=_h(s, "outsider")), 404)
    assert_status(requests.get(url, headers=_h(s, "guest")), 200)

    # 개수 초과 / 제목 초과 / 일반 멤버
    resp = requests.put(url, json={"sections": [{"title": f"s{i}", "content": "", "sortOrder": i} for i in range(11)]}, headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert resp.json()["code"] == "Event_400_21"
    assert_status(requests.put(url, json={"sections": [{"title": "가" * 21, "content": "", "sortOrder": 0}]}, headers=_h(s, "manager")), 400)
    assert_status(requests.put(url, json={"sections": SECTIONS}, headers=_h(s, "guest")), 403)

    c = _checklist(base_url, s, s.event_id)
    assert c["isBasicFilled"] is True and c["isDetailFilled"] is True and c["hasValidTicket"] is False and c["canOpen"] is False


def test_07_ticket_then_open(base_url, s):
    _v1_free_ticket(base_url, s, s.event_id)
    c = _checklist(base_url, s, s.event_id)
    assert c == {"isBasicFilled": True, "isDetailFilled": True, "hasValidTicket": True, "ticketRequired": True, "canOpen": True}

    assert_status(requests.post(f"{base_url}/v2/events/{s.event_id}/open", headers=_h(s, "guest")), 403)
    resp = requests.post(f"{base_url}/v2/events/{s.event_id}/open", headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert get_data(resp) == {"eventId": s.event_id, "status": "OPEN", "displayStatus": "UPCOMING"}
    assert _checklist(base_url, s, s.event_id)["canOpen"] is False


def test_08_public_sections_after_open(base_url, s):
    resp = requests.get(f"{base_url}/v2/events/{s.event_id}/sections")
    assert_status(resp, 200)
    sections = get_data(resp)
    assert sections[0] == {
        "sectionId": sections[0]["sectionId"], "title": "공연 소개", "content": "<p>v2 소개</p>", "contentFormat": "HTML", "sortOrder": 0,
    }
    assert sections[0]["sectionId"]


def test_09_edit_after_open(base_url, s):
    new_end = START + timedelta(minutes=150)
    resp = _patch_basic(base_url, s, s.event_id, {"name": "v2등록후수정", "endAt": _f(new_end), "hasTicket": True})
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["name"] == "v2등록후수정"
    assert data["runTime"] == 150
    assert data["status"] == "OPEN"

    resp = _patch_basic(base_url, s, s.event_id, {"hasTicket": False})
    assert_status(resp, 400)
    assert resp.json()["code"] == "Event_400_18"

    # 등록된 공연의 시작 시각은 현재 이후로만
    past = datetime.now() - timedelta(hours=1)
    resp = _patch_basic(base_url, s, s.event_id, {"startAt": _f(past), "endAt": _f(past + timedelta(hours=2))})
    assert_status(resp, 400)
    assert resp.json()["code"] == "Event_400_24"
    assert_status(_patch_basic(base_url, s, s.event_id, {"startAt": _f(START), "name": "v2등록후수정"}), 200)

    # v1 PATCH basic 은 OPEN 이면 여전히 불가
    resp = requests.patch(
        f"{base_url}/v1/events/{s.event_id}/basic",
        json={"name": "v1", "startAt": _f(START), "runTime": 10, "placeName": "a", "placeAddress": "b", "longitude": 1.0, "latitude": 1.0},
        headers=_h(s, "master"),
    )
    assert_status(resp, 400)
    assert resp.json()["code"] == "Event_400_4"


def test_10_v1_detail_compat(base_url, s):
    resp = requests.get(f"{base_url}/v1/events/{s.event_id}")  # OPEN 이라 비로그인 조회 가능
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["name"] == "v2등록후수정"
    assert data["startAt"] == _f(START)
    assert data["runTime"] == 150
    assert data["endAt"] == _f(START + timedelta(minutes=150))
    assert data["content"] == "<p>v2 소개</p>"
    for hidden in ("hasTicket", "sections", "contacts", "tags"):
        assert hidden not in data

    # v1 details 수정 → v2 공연 소개 섹션 반영 (다른 섹션 유지)
    resp = requests.patch(
        f"{base_url}/v1/events/{s.event_id}/details",
        json={"posterImageKey": issued_event_image_key(base_url, _h(s, "master"), s.event_id), "content": "v1에서 고친 소개"},
        headers=_h(s, "master"),
    )
    assert_status(resp, 200)
    sections = get_data(requests.get(f"{base_url}/v2/events/{s.event_id}/sections"))
    assert [x["content"] for x in sections[:2]] == ["v1에서 고친 소개", "예매 안내"]
    assert [x["contentFormat"] for x in sections[:2]] == ["MARKDOWN", "HTML"]


def test_11_no_ticket_event(base_url, s):
    resp = _create_event(base_url, s, name="v2버스킹", hasTicket=False)
    assert_status(resp, 200)
    s.no_ticket_event_id = get_data(resp)["eventId"]
    _fill(base_url, s, s.no_ticket_event_id)

    c = _checklist(base_url, s, s.no_ticket_event_id)
    assert c == {"isBasicFilled": True, "isDetailFilled": True, "hasValidTicket": False, "ticketRequired": False, "canOpen": True}

    # v1 open 은 hasTicket=false 여도 티켓을 요구 (v1 동작 불변). v1 detail 조건을 위해 포스터+content 를 v1 로 채운다
    resp = requests.post(f"{base_url}/v2/events", json={"hostId": s.host_id, "name": "v1오픈시도", "startAt": _f(START), "endAt": _f(END), "hasTicket": False}, headers=_h(s, "manager"))
    v1_try_id = get_data(resp)["eventId"]
    assert_status(requests.patch(
        f"{base_url}/v1/events/{v1_try_id}/basic",
        json={"name": "v1오픈시도", "startAt": _f(START), "runTime": 60, "placeName": "a", "placeAddress": "b", "longitude": 1.0, "latitude": 1.0},
        headers=_h(s, "master"),
    ), 200)
    assert_status(requests.patch(
        f"{base_url}/v1/events/{v1_try_id}/details",
        json={"posterImageKey": issued_event_image_key(base_url, _h(s, "master"), v1_try_id), "content": "본문"},
        headers=_h(s, "master"),
    ), 200)
    assert_status(requests.patch(f"{base_url}/v1/events/{v1_try_id}/open", headers=_h(s, "master")), 400)
    v1_detail = get_data(requests.get(f"{base_url}/v1/events/{v1_try_id}", headers=_h(s, "master")))
    assert v1_detail["status"] == "준비중"
    assert v1_detail["runTime"] == 60

    resp = requests.post(f"{base_url}/v2/events/{s.no_ticket_event_id}/open", headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert get_data(resp)["status"] == "OPEN"


def test_11b_has_ticket_false_blocked_by_ticket(base_url, s):
    resp = _create_event(base_url, s, name="v2티켓있음")
    event_id = get_data(resp)["eventId"]
    _v1_free_ticket(base_url, s, event_id)
    resp = _patch_basic(base_url, s, event_id, {"hasTicket": False})
    assert_status(resp, 400)
    assert resp.json()["code"] == "Event_400_25"
    assert get_data(requests.get(f"{base_url}/v2/events/{event_id}/manage", headers=_h(s, "master")))["hasTicket"] is True


def test_12_delete_rules(base_url, s):
    assert_status(requests.delete(f"{base_url}/v2/events/{s.event_id}", headers=_h(s, "master")), 400)
    resp = requests.delete(f"{base_url}/v2/events/{s.no_ticket_event_id}", headers=_h(s, "master"))
    assert_status(resp, 400)
    assert resp.json()["code"] == "Event_400_19"

    resp = _create_event(base_url, s, name="v2삭제할공연")
    preparing_id = get_data(resp)["eventId"]
    assert_status(requests.delete(f"{base_url}/v2/events/{preparing_id}", headers=_h(s, "guest")), 403)
    resp = requests.delete(f"{base_url}/v2/events/{preparing_id}", headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert get_data(resp)["status"] == "DELETED"
    assert_status(requests.get(f"{base_url}/v2/events/{preparing_id}/manage", headers=_h(s, "master")), 404)
    assert_status(requests.delete(f"{base_url}/v2/events/{preparing_id}", headers=_h(s, "master")), 404)


def test_13_my_events(base_url, s):
    before = datetime.now().date()
    resp = requests.get(f"{base_url}/v2/me/events", headers=_h(s, "guest"))
    after = datetime.now().date()
    assert_status(resp, 200)
    data = get_data(resp)
    names = [e["name"] for e in data["content"]]
    assert "v2삭제할공연" not in names
    by_id = {e["eventId"]: e for e in data["content"]}
    assert by_id[s.event_id]["displayStatus"] == "UPCOMING"
    # 자정 경계: 요청 전후 날짜 중 하나 기준이면 된다
    assert by_id[s.event_id]["dDay"] in {(START.date() - before).days, (START.date() - after).days}
    assert by_id[s.event_id]["placeName"] == "롤링홀"
    assert by_id[s.event_id]["hostName"].startswith("공연준비")
    ids = [e["eventId"] for e in data["content"]]
    assert ids == sorted(ids, reverse=True)

    resp = requests.get(f"{base_url}/v2/me/events", params={"keyword": "버스킹"}, headers=_h(s, "guest"))
    assert [e["eventId"] for e in get_data(resp)["content"]] == [s.no_ticket_event_id]

    # LIKE 와일드카드는 문자 그대로 (MySQL)
    pct_id = get_data(_create_event(base_url, s, name=f"100%_{RUN[:4]}"))["eventId"]
    for kw in ("%", "_"):
        found = [e["eventId"] for e in get_data(requests.get(f"{base_url}/v2/me/events", params={"keyword": kw}, headers=_h(s, "guest")))["content"]]
        assert found == [pct_id], (kw, found)

    # 비멤버에게는 이 호스트 공연이 없다
    outsider = get_data(requests.get(f"{base_url}/v2/me/events", headers=_h(s, "outsider")))
    assert s.event_id not in [e["eventId"] for e in outsider["content"]]
    assert_status(requests.get(f"{base_url}/v2/me/events"), 401)


def test_14_permission_boundary(base_url, s):
    # 비멤버 / 다른 호스트 마스터(IDOR)는 403
    for who in ("outsider", "other"):
        assert_status(requests.get(f"{base_url}/v2/events/{s.event_id}/manage", headers=_h(s, who)), 403)
        assert_status(_patch_basic(base_url, s, s.event_id, {"name": "탈취"}, who=who), 403)
        assert_status(requests.get(f"{base_url}/v2/events/{s.event_id}/checklist", headers=_h(s, who)), 403)
    assert_status(_patch_basic(base_url, s, s.event_id, {"name": "일반수정"}, who="guest"), 403)
    assert_status(requests.post(
        f"{base_url}/v2/events/{s.event_id}/images", json={"purpose": "SECTION", "extension": "PNG"}, headers=_h(s, "guest")
    ), 403)
    # 다른 호스트로 공연 생성 시도
    assert_status(_create_event(base_url, s, who="manager", host_id=s.other_host_id), 403)
    assert_status(requests.get(f"{base_url}/v2/events/{s.event_id}/manage"), 401)
    assert get_data(requests.get(f"{base_url}/v2/events/{s.event_id}/manage", headers=_h(s, "master")))["name"] == "v2등록후수정"
