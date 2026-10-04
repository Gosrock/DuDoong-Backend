"""
v2 마이페이지 API E2E 테스트 (#729): M-1 프로필 / M-2 수정 / M-3 이미지 / M-4 관심 호스트 / M-5 관람 공연 아카이빙.

시나리오: 호스트 3개(공연 2개 / 공연 없음 / 준비중 공연만) 준비 → 팬이 무료 티켓 주문 → 호스트 스캔으로 입장 →
관심 호스트(대표 공연·필터·언팔로우) → v1 상태 변경(OPEN → CALCULATING)으로 공연 종료 → 아카이빙(연도 탭·중복 제거·주문 uuid).
공연 종료는 DB 를 직접 바꾸지 않고 v1 `PATCH /v1/events/{id}/status` 로 한다 (호스트 스캔은 CALCULATING 에서도 된다).

재실행해도 충돌하지 않도록 유저 이메일에 실행마다 다른 접미사를 붙인다. DB 직접 접근은 하지 않는다.
"""
import uuid
from datetime import datetime, timedelta

import pytest
import requests

from conftest import assert_status, get_data

RUN = uuid.uuid4().hex[:8]
FMT = "%Y.%m.%d %H:%M"
NEAR = (datetime.now() + timedelta(days=30)).replace(hour=18, minute=0, second=0, microsecond=0)
FAR = (datetime.now() + timedelta(days=100)).replace(hour=18, minute=0, second=0, microsecond=0)
PLACE = {"name": "롤링홀", "address": "서울 마포구 어울마당로 35", "latitude": 37.548369, "longitude": 126.920036}
SECTIONS = [{"title": "공연 소개", "content": "<p>마이페이지 테스트</p>", "sortOrder": 0}]


class MyPageState:
    tokens: dict = {}
    emails: dict = {}
    user_ids: dict = {}
    host_a: int = 0
    host_empty: int = 0
    host_preparing: int = 0
    near_event: int = 0
    far_event: int = 0
    near_ticket: int = 0
    far_ticket: int = 0
    orders: dict = {}


@pytest.fixture(scope="module")
def s():
    return MyPageState()


def _h(s, who):
    return {"Authorization": f"Bearer {s.tokens[who]}"}


def _code(resp):
    return resp.json().get("code")


def _login(base_url, email, name):
    resp = requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={"email": email, "name": name, "phoneNumber": "010-0000-0000", "profileImage": None, "marketingAgree": False},
    )
    assert_status(resp, 200)
    return get_data(resp)["accessToken"]


def _me(base_url, s, who):
    resp = requests.get(f"{base_url}/v2/me", headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)


def _following(base_url, s, who, **params):
    resp = requests.get(f"{base_url}/v2/me/following-hosts", params=params, headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)


def _archive(base_url, s, who, **params):
    resp = requests.get(f"{base_url}/v2/me/archive", params=params, headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)


def _host(base_url, s, who, name):
    resp = requests.post(f"{base_url}/v2/hosts", json={"name": name, "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)["hostId"]


def _registered_event(base_url, s, host_id, name, start):
    """v2 로 공연 생성 → 무료 티켓 → 포스터·장소·섹션 → 등록(OPEN). (eventId, ticketItemId)"""
    h = _h(s, "master")
    resp = requests.post(
        f"{base_url}/v2/events",
        json={"hostId": host_id, "name": name, "startAt": start.strftime(FMT), "endAt": (start + timedelta(minutes=120)).strftime(FMT), "hasTicket": True},
        headers=h,
    )
    assert_status(resp, 200)
    event_id = get_data(resp)["eventId"]
    ev = f"{base_url}/v2/events/{event_id}"
    resp = requests.post(f"{ev}/ticket-items", json={
        "payType": "FREE", "name": "무료", "description": "무료", "price": 0, "supplyCount": 10, "account": None,
        "approvalRequired": False, "isQuantityPublic": True, "purchaseLimit": 4, "saleStartAt": None, "saleEndAt": None,
    }, headers=h)
    assert_status(resp, 200)
    ticket_id = get_data(resp)["ticketItemId"]
    key = get_data(requests.post(f"{ev}/images", json={"purpose": "POSTER", "extension": "PNG"}, headers=h))["key"]
    assert_status(requests.patch(f"{ev}/basic", json={"posterImageKey": key, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]}, headers=h), 200)
    assert_status(requests.put(f"{ev}/sections", json=SECTIONS, headers=h), 200)
    assert_status(requests.post(f"{ev}/open", headers=h), 200)
    return event_id, ticket_id


def _free_order(base_url, s, who, event_id, ticket_id, quantity):
    resp = requests.post(f"{base_url}/v2/orders", json={
        "eventId": event_id, "ticketItemId": ticket_id, "quantity": quantity,
        "options": {"applyToAll": True, "answers": []}, "perTicketOptions": None,
        "paymentMethod": "FREE", "depositorName": None, "agreeRefundPolicy": True,
    }, headers=_h(s, who))
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["status"] == "APPROVED" and len(data["issuedTickets"]) == quantity
    return data["orderUuid"]


def _ticket_uuids(base_url, s, event_id, order_uuid):
    resp = requests.get(f"{base_url}/v2/events/{event_id}/orders/{order_uuid}", headers=_h(s, "master"))
    assert_status(resp, 200)
    return [t["ticketUuid"] for t in get_data(resp)["issuedTickets"]]


def _scan(base_url, s, event_id, ticket_uuid):
    resp = requests.post(f"{base_url}/v2/events/{event_id}/check-ins", json={"ticketUuid": ticket_uuid}, headers=_h(s, "master"))
    assert_status(resp, 200)
    assert get_data(resp)["result"] == "ENTERED"


def _end_event(base_url, s, event_id):
    resp = requests.patch(f"{base_url}/v1/events/{event_id}/status", json={"status": "CALCULATING"}, headers=_h(s, "master"))
    assert_status(resp, 200)


def test_01_setup(base_url, s):
    for who, name in [("master", "마이마스터"), ("fan", "마이팬"), ("other", "마이남"), ("v1name", "v1이름"), ("v2name", "v2이름")]:
        email = f"v2mypage-{who}-{RUN}@dudoong.com"
        s.tokens[who], s.emails[who] = _login(base_url, email, name), email
        s.user_ids[who] = get_data(requests.get(f"{base_url}/v1/users/me", headers=_h(s, who)))["userId"]
    s.host_a = _host(base_url, s, "master", f"마이A{RUN[:4]}")
    s.host_empty = _host(base_url, s, "master", f"마이빈{RUN[:4]}")
    s.host_preparing = _host(base_url, s, "master", f"마이준{RUN[:4]}")
    s.near_event, s.near_ticket = _registered_event(base_url, s, s.host_a, "가까운공연", NEAR)
    s.far_event, s.far_ticket = _registered_event(base_url, s, s.host_a, "먼공연", FAR)
    resp = requests.post(
        f"{base_url}/v2/events",
        json={"hostId": s.host_preparing, "name": "준비중", "startAt": NEAR.strftime(FMT), "endAt": (NEAR + timedelta(hours=2)).strftime(FMT), "hasTicket": True},
        headers=_h(s, "master"),
    )
    assert_status(resp, 200)


def test_02_auth_required(base_url):
    for method, path in [("GET", "/v2/me"), ("PATCH", "/v2/me"), ("POST", "/v2/me/images"), ("GET", "/v2/me/following-hosts"), ("GET", "/v2/me/archive")]:
        assert_status(requests.request(method, f"{base_url}{path}", json={}), 401)


def test_03_me_profile_and_hosts(base_url, s):
    fan = _me(base_url, s, "fan")
    v1 = get_data(requests.get(f"{base_url}/v1/users/me", headers=_h(s, "fan")))
    assert fan["userId"] == v1["userId"] and fan["name"] == "마이팬"
    assert fan["email"] == v1["email"] == s.emails["fan"]
    assert fan["hosts"] == [] and fan["hostCount"] == 0

    master = _me(base_url, s, "master")
    # 합류 최신순: 마지막에 만든 호스트가 앞
    assert [h["hostId"] for h in master["hosts"]] == [s.host_preparing, s.host_empty, s.host_a]
    assert all(h["myRole"] == "MASTER" for h in master["hosts"]) and master["hostCount"] == 3


def test_04_update_name_matches_v1(base_url, s):
    for name in ["두둥", "ab", "abcdefg", "a", "abcdefgh", "", "   ", "\t\t"]:
        v1 = requests.patch(f"{base_url}/v1/users/me/name", json={"name": name}, headers=_h(s, "v1name"))
        v2 = requests.patch(f"{base_url}/v2/me", json={"name": name}, headers=_h(s, "v2name"))
        assert v1.status_code == v2.status_code, f"{name!r}: v1={v1.status_code} v2={v2.status_code} {v2.text[:200]}"
        if v2.status_code == 200:
            assert get_data(v2)["name"] == name
    # v1 은 500 이 나는 전각 공백만인 이름은 v2 에서 400
    resp = requests.patch(f"{base_url}/v2/me", json={"name": "　　"}, headers=_h(s, "v2name"))
    assert_status(resp, 400)
    assert _code(resp) == "USER_400_4"
    assert _me(base_url, s, "v2name")["name"] == "abcdefg"


def test_05_profile_image(base_url, s):
    resp = requests.post(f"{base_url}/v2/me/images", json={"extension": "PNG"}, headers=_h(s, "fan"))
    assert_status(resp, 200)
    upload = get_data(resp)
    assert f"/user/{s.user_ids['fan']}/" in upload["key"] and upload["presignedUrl"]
    assert_status(requests.post(f"{base_url}/v2/me/images", json={"extension": "GIF"}, headers=_h(s, "fan")), 400)

    data = get_data(requests.patch(f"{base_url}/v2/me", json={"profileImageKey": upload["key"]}, headers=_h(s, "fan")))
    assert data["profileImageUrl"].endswith(upload["key"]) and data["name"] == "마이팬"
    # v1 내 정보에도 같은 이미지
    assert get_data(requests.get(f"{base_url}/v1/users/me", headers=_h(s, "fan")))["profileImage"].endswith(upload["key"])

    # 남에게 발급된 key 는 400
    other_key = get_data(requests.post(f"{base_url}/v2/me/images", json={"extension": "PNG"}, headers=_h(s, "other")))["key"]
    resp = requests.patch(f"{base_url}/v2/me", json={"profileImageKey": other_key}, headers=_h(s, "fan"))
    assert_status(resp, 400)
    assert _code(resp) == "USER_400_5"

    # 빈 문자열 = 기본 이미지
    data = get_data(requests.patch(f"{base_url}/v2/me", json={"profileImageKey": ""}, headers=_h(s, "fan")))
    assert data["profileImageUrl"] is None


def test_06_following_hosts_upcoming(base_url, s):
    for host_id in (s.host_a, s.host_empty, s.host_preparing):
        assert_status(requests.put(f"{base_url}/v2/hosts/{host_id}/follow", headers=_h(s, "fan")), 200)
    data = _following(base_url, s, "fan")
    assert [h["hostId"] for h in data["content"]] == [s.host_preparing, s.host_empty, s.host_a]
    assert data["totalElements"] == 3
    rep = {h["hostId"]: h["representativeEvent"] for h in data["content"]}
    assert rep[s.host_a]["eventId"] == s.near_event and rep[s.host_a]["displayStatus"] == "UPCOMING"
    assert rep[s.host_a]["dDay"] == (NEAR.date() - datetime.now().date()).days
    assert rep[s.host_a]["posterImageUrl"]
    # 공연 없음 / 준비중 공연만 → 대표 공연 없음
    assert rep[s.host_empty] is None and rep[s.host_preparing] is None

    assert [h["hostId"] for h in _following(base_url, s, "fan", status="ACTIVE")["content"]] == [s.host_a]
    assert _following(base_url, s, "fan", status="ENDED")["content"] == []
    page = _following(base_url, s, "fan", size=2, page=1)
    assert [h["hostId"] for h in page["content"]] == [s.host_a] and page["hasNext"] is False
    # 남의 관심 호스트는 보이지 않는다
    assert _following(base_url, s, "other")["content"] == []


def test_07_unfollow_from_list(base_url, s):
    assert_status(requests.delete(f"{base_url}/v2/hosts/{s.host_empty}/follow", headers=_h(s, "fan")), 200)
    assert [h["hostId"] for h in _following(base_url, s, "fan")["content"]] == [s.host_preparing, s.host_a]


def test_08_enter_events(base_url, s):
    s.orders["near"] = _free_order(base_url, s, "fan", s.near_event, s.near_ticket, 2)
    s.orders["far"] = _free_order(base_url, s, "fan", s.far_event, s.far_ticket, 1)
    s.orders["other"] = _free_order(base_url, s, "other", s.near_event, s.near_ticket, 1)
    for uuid_ in _ticket_uuids(base_url, s, s.near_event, s.orders["near"]):
        _scan(base_url, s, s.near_event, uuid_)
    # 아직 끝나지 않은 공연은 입장했어도 아카이빙에 없다
    archive = _archive(base_url, s, "fan")
    assert archive["years"] == [] and archive["events"]["content"] == []


def test_09_archive_after_end(base_url, s):
    _end_event(base_url, s, s.near_event)
    _end_event(base_url, s, s.far_event)
    # 먼 공연은 종료 뒤(정산중) 지각 입장
    _scan(base_url, s, s.far_event, _ticket_uuids(base_url, s, s.far_event, s.orders["far"])[0])

    archive = _archive(base_url, s, "fan")
    years = sorted({NEAR.year, FAR.year}, reverse=True)
    assert archive["years"] == years
    content = archive["events"]["content"]
    # 같은 공연 2장 입장 → 1건, 최근(시작 늦은) 공연 순
    assert [e["eventId"] for e in content] == [s.far_event, s.near_event]
    assert archive["events"]["totalElements"] == 2
    near = content[1]
    assert near["name"] == "가까운공연" and near["posterImageUrl"] and near["startAt"] == NEAR.strftime(FMT)
    assert near["host"] == {"hostId": s.host_a, "name": f"마이A{RUN[:4]}"}
    assert near["orderUuid"] == s.orders["near"]
    assert_status(requests.get(f"{base_url}/v2/me/orders/{near['orderUuid']}", headers=_h(s, "fan")), 200)

    in_near_year = _archive(base_url, s, "fan", year=NEAR.year)["events"]["content"]
    expected = [e for e in (s.far_event, s.near_event) if (FAR if e == s.far_event else NEAR).year == NEAR.year]
    assert [e["eventId"] for e in in_near_year] == expected
    assert_status(requests.get(f"{base_url}/v2/me/archive", params={"year": 0}, headers=_h(s, "fan")), 400)

    # 주문만 하고 입장하지 않은 사람은 비어 있다
    other = _archive(base_url, s, "other")
    assert other["years"] == [] and other["events"]["content"] == []


def test_10_following_after_end(base_url, s):
    data = _following(base_url, s, "fan")
    rep = {h["hostId"]: h["representativeEvent"] for h in data["content"]}
    # 끝난 공연만 남으면 가장 최근에 끝난 공연(먼 공연)
    assert rep[s.host_a]["eventId"] == s.far_event and rep[s.host_a]["displayStatus"] == "PAST" and rep[s.host_a]["dDay"] is None
    assert [h["hostId"] for h in _following(base_url, s, "fan", status="ENDED")["content"]] == [s.host_a]
    assert _following(base_url, s, "fan", status="ACTIVE")["content"] == []
