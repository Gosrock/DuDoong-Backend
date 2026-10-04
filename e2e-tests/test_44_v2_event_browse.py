"""
v2 사용자 앱 공연 탐색 API E2E 테스트 (#716).

시나리오: 호스트·공연(OPEN 2개 / 정산중→지난공연 1개 / 준비중 1개)·태그·티켓 준비 →
비로그인으로 홈 / 리스트(검색어·태그 OR·AND·지난공연 포함·페이징) / 상세(문의처 대체 포함) / 섹션 / 티켓 →
준비중 공연 404 → 판매 중단·판매 전 티켓 미노출, 계좌 미노출.

재실행해도 충돌하지 않도록 이름·이메일에 실행마다 다른 토큰을 넣고, 리스트는 그 토큰으로 검색해 격리한다. DB 직접 접근은 하지 않는다.
"""
import uuid
from datetime import datetime, timedelta

import pytest
import requests

from conftest import assert_status, get_data

RUN = uuid.uuid4().hex[:6]
FMT = "%Y.%m.%d %H:%M"
BASE = (datetime.now() + timedelta(days=20)).replace(hour=19, minute=0, second=0, microsecond=0)
PLACE = {"name": "롤링홀", "address": "서울 마포구 어울마당로 35", "latitude": 37.548369, "longitude": 126.920036}
SECTIONS = [{"title": "공연 소개", "content": "<p>탐색 테스트</p>", "sortOrder": 0}]
ACCOUNT = {"bank": "신한은행", "holder": "고스락", "number": "110-987-654321"}


class BrowseState:
    tokens: dict = {}
    host_id: int = 0
    band_host_id: int = 0
    tags: dict = {}
    open_a: int = 0      # +3일, 정기·락, 공연 문의처 있음
    open_b: int = 0      # +2일, 단독·힙합, 등록 후 문의처 비움 → 호스트 연락처 대체
    band_c: int = 0      # +4일, 다른 호스트(호스트명에만 토큰)
    closed: int = 0      # 정산중 → 지난공연, 정기·락
    preparing: int = 0   # 준비중, 정기·락
    free_id: int = 0
    dudoong_id: int = 0
    suspended_id: int = 0
    future_id: int = 0


@pytest.fixture(scope="module")
def s():
    return BrowseState()


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
    return get_data(resp)["accessToken"]


def _free(name, **overrides):
    body = {
        "payType": "FREE", "name": name, "description": "무료 입장", "price": 0, "supplyCount": 10, "account": None,
        "approvalRequired": False, "isQuantityPublic": True, "purchaseLimit": 2, "saleStartAt": None, "saleEndAt": None,
    }
    body.update(overrides)
    return body


def _ticket(base_url, s, who, event_id, body):
    resp = requests.post(f"{base_url}/v2/events/{event_id}/ticket-items", json=body, headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)["ticketItemId"]


def _event(base_url, s, who, host_id, name, start, tag_names, contacts=None):
    """생성 → 포스터·장소·문의처·태그·섹션 → 티켓 1개. 등록은 호출 측에서"""
    resp = requests.post(
        f"{base_url}/v2/events",
        json={"hostId": host_id, "name": name, "startAt": _f(start), "endAt": _f(start + timedelta(minutes=150)), "hasTicket": True},
        headers=_h(s, who),
    )
    assert_status(resp, 200)
    event_id = get_data(resp)["eventId"]
    key = get_data(requests.post(f"{base_url}/v2/events/{event_id}/images", json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, who)))["key"]
    body = {
        "posterImageKey": key, "place": PLACE, "tagIds": [s.tags[n] for n in tag_names],
        "contacts": contacts or [{"type": "INSTAGRAM", "value": "@browse"}],
    }
    assert_status(requests.patch(f"{base_url}/v2/events/{event_id}/basic", json=body, headers=_h(s, who)), 200)
    assert_status(requests.put(f"{base_url}/v2/events/{event_id}/sections", json=SECTIONS, headers=_h(s, who)), 200)
    return event_id


def _open(base_url, s, who, event_id):
    assert_status(requests.post(f"{base_url}/v2/events/{event_id}/open", headers=_h(s, who)), 200)


def _list(base_url, keyword, tag_ids=None, include_past=True, page=0, size=50, expect=200):
    params = {"keyword": keyword, "includePast": str(include_past).lower(), "page": page, "size": size}
    if tag_ids:
        params["tagIds"] = ",".join(str(t) for t in tag_ids)
    resp = requests.get(f"{base_url}/v2/events", params=params)  # 비로그인
    assert_status(resp, expect)
    return get_data(resp) if expect == 200 else resp


def _ids(page):
    return [e["eventId"] for e in page["content"]]


def test_01_setup(base_url, s):
    for who in ["master", "band"]:
        s.tokens[who] = _login(base_url, f"v2browse-{who}-{RUN}@dudoong.com", f"탐색{who}")
    for who, attr, name in [("master", "host_id", f"탐색{RUN}"), ("band", "band_host_id", f"밴드{RUN}")]:
        resp = requests.post(f"{base_url}/v2/hosts", json={"name": name, "contacts": [{"type": "EMAIL", "value": f"{who}@dudoong.com"}]}, headers=_h(s, who))
        assert_status(resp, 200)
        setattr(s, attr, get_data(resp)["hostId"])

    resp = requests.get(f"{base_url}/v2/tags")  # 비로그인
    assert_status(resp, 200)
    s.tags = {t["name"]: t["tagId"] for group in get_data(resp) for t in group["tags"]}
    assert {"정기공연", "단독공연", "락밴드", "힙합"} <= s.tags.keys()

    s.open_a = _event(base_url, s, "master", s.host_id, f"탐색{RUN}-A", BASE + timedelta(days=3), ["정기공연", "락밴드"])
    s.free_id = _ticket(base_url, s, "master", s.open_a, _free("무료"))
    s.dudoong_id = _ticket(base_url, s, "master", s.open_a, {
        "payType": "DUDOONG", "name": "일반", "description": "일반 입장", "price": 8000, "supplyCount": None, "account": ACCOUNT,
        "approvalRequired": True, "isQuantityPublic": False, "purchaseLimit": None, "saleStartAt": None, "saleEndAt": None,
    })
    s.suspended_id = _ticket(base_url, s, "master", s.open_a, _free("중단"))
    s.future_id = _ticket(base_url, s, "master", s.open_a, _free("판매전", saleStartAt=_f(datetime.now() + timedelta(days=1))))
    assert_status(requests.post(f"{base_url}/v2/events/{s.open_a}/ticket-items/{s.suspended_id}/suspend", headers=_h(s, "master")), 200)
    _open(base_url, s, "master", s.open_a)

    s.open_b = _event(base_url, s, "master", s.host_id, f"탐색{RUN}-B", BASE + timedelta(days=2), ["단독공연", "힙합"])
    _ticket(base_url, s, "master", s.open_b, _free("무료"))
    _open(base_url, s, "master", s.open_b)
    # 등록 후 공연 문의처를 비움 → 상세는 호스트 연락처로 대체
    assert_status(requests.patch(f"{base_url}/v2/events/{s.open_b}/basic", json={"contacts": []}, headers=_h(s, "master")), 200)

    s.band_c = _event(base_url, s, "band", s.band_host_id, "다른호스트공연", BASE + timedelta(days=4), [])
    _ticket(base_url, s, "band", s.band_c, _free("무료"))
    _open(base_url, s, "band", s.band_c)

    s.closed = _event(base_url, s, "master", s.host_id, f"탐색{RUN}-C", BASE + timedelta(days=1), ["정기공연", "락밴드"])
    _ticket(base_url, s, "master", s.closed, _free("무료"))
    _open(base_url, s, "master", s.closed)
    for status in ["CALCULATING", "CLOSED"]:
        resp = requests.patch(f"{base_url}/v1/events/{s.closed}/status", json={"status": status}, headers=_h(s, "master"))
        assert_status(resp, 200)

    s.preparing = _event(base_url, s, "master", s.host_id, f"탐색{RUN}-P", BASE + timedelta(days=1), ["정기공연", "락밴드"])
    _ticket(base_url, s, "master", s.preparing, _free("무료"))


def test_02_home(base_url, s):
    resp = requests.get(f"{base_url}/v2/home")
    assert_status(resp, 200)
    events = get_data(resp)["events"]
    assert len(events) <= 10
    starts = [datetime.strptime(e["startAt"], FMT) for e in events]
    assert starts == sorted(starts)
    assert all(t > datetime.now() - timedelta(minutes=1) for t in starts)
    ids = [e["eventId"] for e in events]
    assert s.preparing not in ids and s.closed not in ids
    mine = next((e for e in events if e["eventId"] == s.open_b), None)
    if mine:  # 다른 E2E 공연이 앞서면 10개 밖일 수 있다
        assert mine["hostName"] == f"탐색{RUN}"
        assert mine["placeName"] == "롤링홀"
        assert mine["posterImageUrl"]


def test_03_list_sort_and_include_past(base_url, s):
    # 다가오는 공연 임박순 → 지난 공연. 준비중 제외. 호스트명(밴드{RUN})으로도 걸린다
    assert _ids(_list(base_url, RUN)) == [s.open_b, s.open_a, s.band_c, s.closed]
    upcoming = _list(base_url, RUN, include_past=False)
    assert _ids(upcoming) == [s.open_b, s.open_a, s.band_c]
    assert all(e["displayStatus"] == "UPCOMING" for e in upcoming["content"])
    page = _list(base_url, RUN)
    assert [e["displayStatus"] for e in page["content"]] == ["UPCOMING", "UPCOMING", "UPCOMING", "PAST"]
    a = next(e for e in page["content"] if e["eventId"] == s.open_a)
    assert a["hostName"] == f"탐색{RUN}"
    assert a["placeName"] == "롤링홀"
    assert a["endAt"] == _f(BASE + timedelta(days=3, minutes=150))
    assert [(t["category"], t["name"]) for t in a["tags"]] == [("EVENT_TYPE", "정기공연"), ("GENRE", "락밴드")]


def test_04_list_keyword(base_url, s):
    assert _ids(_list(base_url, f"밴드{RUN}")) == [s.band_c]
    assert _ids(_list(base_url, f"{RUN}-A")) == [s.open_a]
    assert _ids(_list(base_url, RUN.upper())) == _ids(_list(base_url, RUN))
    # LIKE 와일드카드는 문자 그대로
    assert _ids(_list(base_url, f"%{RUN}")) == []
    assert _ids(_list(base_url, f"_{RUN}")) == []


def test_05_list_tags(base_url, s):
    t = s.tags
    assert _ids(_list(base_url, RUN, [t["정기공연"]])) == [s.open_a, s.closed]
    # 같은 분류 OR
    assert _ids(_list(base_url, RUN, [t["정기공연"], t["단독공연"]])) == [s.open_b, s.open_a, s.closed]
    # 분류끼리 AND
    assert _ids(_list(base_url, RUN, [t["단독공연"], t["락밴드"]])) == []
    assert _ids(_list(base_url, RUN, [t["정기공연"], t["단독공연"], t["락밴드"]])) == [s.open_a, s.closed]
    assert _ids(_list(base_url, RUN, [t["정기공연"], t["락밴드"]], include_past=False)) == [s.open_a]
    resp = _list(base_url, RUN, [t["정기공연"], 99999999], expect=400)
    assert resp.json().get("code") == "Event_400_23"


def test_06_list_paging(base_url, s):
    p0 = _list(base_url, RUN, size=3, page=0)
    p1 = _list(base_url, RUN, size=3, page=1)
    assert _ids(p0) + _ids(p1) == [s.open_b, s.open_a, s.band_c, s.closed]
    assert p0["totalElements"] == 4 and p0["totalPages"] == 2 and p0["hasNext"] is True
    assert p1["hasNext"] is False
    assert_status(requests.get(f"{base_url}/v2/events", params={"size": 51}), 400)


def test_07_detail(base_url, s):
    resp = requests.get(f"{base_url}/v2/events/{s.open_a}")
    assert_status(resp, 200)
    d = get_data(resp)
    assert d["name"] == f"탐색{RUN}-A"
    assert d["startAt"] == _f(BASE + timedelta(days=3)) and d["runTime"] == 150
    assert d["place"]["name"] == "롤링홀" and d["place"]["latitude"] == PLACE["latitude"]
    assert d["host"]["hostId"] == s.host_id and d["host"]["name"] == f"탐색{RUN}"
    assert d["contacts"] == [{"type": "INSTAGRAM", "value": "@browse"}]
    assert [(t["category"], t["name"]) for t in d["tags"]] == [("EVENT_TYPE", "정기공연"), ("GENRE", "락밴드")]
    assert d["displayStatus"] == "UPCOMING"
    for hidden in ["status", "checkInToken", "myRole", "checklist"]:
        assert hidden not in d
    for hidden in ["slackUrl", "members"]:
        assert hidden not in d["host"]

    # 공연 문의처가 없으면 호스트 연락처
    b = get_data(requests.get(f"{base_url}/v2/events/{s.open_b}"))
    assert b["contacts"] == [{"type": "EMAIL", "value": "master@dudoong.com"}]

    c = get_data(requests.get(f"{base_url}/v2/events/{s.closed}"))
    assert c["displayStatus"] == "PAST"


def test_08_preparing_hidden(base_url, s):
    for path in [f"/v2/events/{s.preparing}", f"/v2/events/{s.preparing}/ticket-items", f"/v2/events/{s.preparing}/sections"]:
        resp = requests.get(f"{base_url}{path}")
        assert_status(resp, 404)
    # 멤버(마스터)여도 공개 상세·티켓은 404
    assert_status(requests.get(f"{base_url}/v2/events/{s.preparing}", headers=_h(s, "master")), 404)
    assert_status(requests.get(f"{base_url}/v2/events/{s.preparing}/ticket-items", headers=_h(s, "master")), 404)


def test_09_sections(base_url, s):
    resp = requests.get(f"{base_url}/v2/events/{s.open_a}/sections")
    assert_status(resp, 200)
    assert [sec["title"] for sec in get_data(resp)] == ["공연 소개"]


def test_10_tickets(base_url, s):
    resp = requests.get(f"{base_url}/v2/events/{s.open_a}/ticket-items")
    assert_status(resp, 200)
    items = get_data(resp)
    assert [i["ticketItemId"] for i in items] == [s.free_id, s.dudoong_id]
    free, dudoong = items
    assert free["payType"] == "FREE" and free["approvalRequired"] is False
    assert free["remaining"] == 10 and free["isSoldOut"] is False and free["purchaseLimit"] == 2
    assert free["isPurchasable"] is True and dudoong["isPurchasable"] is True
    assert dudoong["payType"] == "DUDOONG" and dudoong["approvalRequired"] is True and dudoong["price"] == 8000
    assert dudoong["remaining"] is None and dudoong["purchaseLimit"] is None
    # 계좌 미노출
    assert "account" not in dudoong
    assert ACCOUNT["number"] not in resp.text and ACCOUNT["bank"] not in resp.text

    # 판매 재개하면 보인다
    assert_status(requests.post(f"{base_url}/v2/events/{s.open_a}/ticket-items/{s.suspended_id}/resume", headers=_h(s, "master")), 200)
    ids = [i["ticketItemId"] for i in get_data(requests.get(f"{base_url}/v2/events/{s.open_a}/ticket-items"))]
    assert ids == [s.free_id, s.dudoong_id, s.suspended_id]
    assert s.future_id not in ids

    # 지난 공연: 판매 중 티켓 목록은 보이지만 구매 불가
    closed = get_data(requests.get(f"{base_url}/v2/events/{s.closed}/ticket-items"))
    assert len(closed) == 1 and closed[0]["isPurchasable"] is False
