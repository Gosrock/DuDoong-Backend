"""
v2 알림센터 E2E 테스트 (#714).

시나리오: 호스트 생성 → 멤버 추가(매니저·일반) → 추가된 유저 알림 → 승인형 티켓 공연 등록 → 다른 유저 주문 →
호스트 마스터·매니저 알림(일반 멤버 없음) → v2 승인 → 주문자 알림 → v2 거절(사유) / v1 거절 → 주문자 알림 →
승인 후 취소는 거절 알림 없음 → 목록·페이징 → 안읽음 수 → 읽음(남의 id 무시, 멱등) → 전체 읽음 멱등 → 비로그인 401.

알림 저장은 커밋 후 비동기라 조회는 저장될 때까지 기다린다(최대 10초). 재실행해도 충돌하지 않도록 이메일에 실행마다 다른 접미사를 붙인다.
"""
import time
import uuid
from datetime import datetime, timedelta

import pytest
import requests

from conftest import assert_status, get_data

RUN = uuid.uuid4().hex[:8]
FMT = "%Y.%m.%d %H:%M"
START = (datetime.now() + timedelta(days=30)).replace(hour=18, minute=0, second=0, microsecond=0)
END = START + timedelta(minutes=120)
PLACE = {"name": "롤링홀", "address": "서울 마포구 어울마당로 35", "latitude": 37.548369, "longitude": 126.920036}
ACCOUNT = {"bankName": "신한은행", "accountHolder": "고스락", "accountNumber": "110-123-456789"}
BUYERS = ["approved", "refused", "v1refused", "canceled"]


class NState:
    tokens: dict = {}
    emails: dict = {}
    host_id: int = 0
    host_name: str = ""
    event_id: int = 0
    ticket_id: int = 0
    orders: dict = {}


@pytest.fixture(scope="module")
def s():
    return NState()


def _h(s, who):
    return {"Authorization": f"Bearer {s.tokens[who]}"}


def _login(base_url, email, name):
    resp = requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={"email": email, "name": name, "phoneNumber": "010-0000-0000", "profileImage": None, "marketingAgree": False},
    )
    assert_status(resp, 200)
    return get_data(resp)["accessToken"]


def _list(base_url, s, who, **params):
    resp = requests.get(f"{base_url}/v2/me/notifications", params=params, headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)


def _unread(base_url, s, who):
    resp = requests.get(f"{base_url}/v2/me/notifications/unread-count", headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)["count"]


def _of_type(base_url, s, who, type_):
    return [n for n in _list(base_url, s, who, size=100)["content"] if n["type"] == type_]


def _wait(base_url, s, who, type_, count=1):
    deadline = time.time() + 10
    while time.time() < deadline:
        found = _of_type(base_url, s, who, type_)
        if len(found) >= count:
            return found
        time.sleep(0.2)
    raise AssertionError(f"{who} 의 {type_} 알림 {count}건이 저장되지 않음 (현재 {len(_of_type(base_url, s, who, type_))}건)")


def _v1_order(base_url, s, who):
    cart = requests.post(f"{base_url}/v1/carts", json={"items": [{"itemId": s.ticket_id, "quantity": 1, "options": []}]}, headers=_h(s, who))
    assert cart.status_code in (200, 201), cart.text[:300]
    resp = requests.post(f"{base_url}/v1/orders/", json={"cartId": get_data(cart)["cartId"], "couponId": None}, headers=_h(s, who))
    assert resp.status_code in (200, 201), resp.text[:300]
    return get_data(resp)["orderId"]


def _ev(base_url, s, path):
    return f"{base_url}/v2/events/{s.event_id}{path}"


def test_01_host_member_added(base_url, s):
    for who, name in [("master", "알림마스터"), ("manager", "알림매니저"), ("guest", "알림일반"), ("outsider", "알림외부")] + [(b, f"알림구매{i}") for i, b in enumerate(BUYERS)]:
        s.emails[who] = f"v2noti-{who}-{RUN}@dudoong.com"
        s.tokens[who] = _login(base_url, s.emails[who], name)

    s.host_name = f"알림{RUN[:5]}"
    resp = requests.post(f"{base_url}/v2/hosts", json={"name": s.host_name, "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    s.host_id = get_data(resp)["hostId"]
    resp = requests.post(
        f"{base_url}/v2/hosts/{s.host_id}/members",
        json={"members": [{"email": s.emails["manager"], "role": "MANAGER"}, {"email": s.emails["guest"], "role": "GUEST"}]},
        headers=_h(s, "master"),
    )
    assert_status(resp, 200)

    for who, role in [("manager", "매니저"), ("guest", "게스트")]:
        n = _wait(base_url, s, who, "HOST_MEMBER_ADDED")
        assert len(n) == 1
        n = n[0]
        assert n["target"] == {"type": "HOST", "targetId": str(s.host_id), "eventId": None}
        assert s.host_name in n["body"] and role in n["body"], n["body"]
        assert n["extra"]["hostName"] == s.host_name
        assert n["isRead"] is False
    # 추가한 마스터는 알림 없음
    assert _list(base_url, s, "master")["content"] == []


def test_02_order_pending_approve(base_url, s):
    resp = requests.post(
        f"{base_url}/v2/events",
        json={"hostId": s.host_id, "name": "알림공연", "startAt": START.strftime(FMT), "endAt": END.strftime(FMT), "hasTicket": True},
        headers=_h(s, "manager"),
    )
    assert_status(resp, 200)
    s.event_id = get_data(resp)["eventId"]
    resp = requests.post(
        _ev(base_url, s, "/ticket-items"),
        json={"payType": "DUDOONG", "name": "일반", "description": "일반 입장", "price": 5000, "supplyCount": 30, "account": ACCOUNT,
              "approvalRequired": True, "isQuantityPublic": True, "purchaseLimit": 4, "saleStartAt": None, "saleEndAt": None},
        headers=_h(s, "manager"),
    )
    assert_status(resp, 200)
    s.ticket_id = get_data(resp)["ticketItemId"]
    key = get_data(requests.post(_ev(base_url, s, "/images"), json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "manager")))["key"]
    assert_status(requests.patch(_ev(base_url, s, "/basic"), json={"posterImageKey": key, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]}, headers=_h(s, "manager")), 200)
    assert_status(requests.put(_ev(base_url, s, "/sections"), json={"sections": [{"title": "공연 소개", "content": "<p>알림</p>", "sortOrder": 0}]}, headers=_h(s, "manager")), 200)
    assert_status(requests.post(_ev(base_url, s, "/open"), headers=_h(s, "manager")), 200)

    for who in BUYERS:
        s.orders[who] = _v1_order(base_url, s, who)

    for who in ("master", "manager"):
        found = _wait(base_url, s, who, "ORDER_PENDING_APPROVE", count=len(BUYERS))
        assert {n["target"]["targetId"] for n in found} == set(s.orders.values())
        assert {n["target"]["type"] for n in found} == {"ORDER"}
        assert {n["target"]["eventId"] for n in found} == {s.event_id}
        assert all("알림공연" in n["body"] for n in found)
    # 일반 멤버·외부인·주문자에게는 없음 (마스터·매니저 알림과 같은 문장으로 저장되므로 이미 결정됨)
    assert _of_type(base_url, s, "guest", "ORDER_PENDING_APPROVE") == []
    assert _list(base_url, s, "outsider")["content"] == []
    assert _list(base_url, s, "approved")["content"] == []


def test_03_approved_refused(base_url, s):
    o = s.orders
    for who in ("approved", "canceled"):
        assert_status(requests.post(_ev(base_url, s, f"/orders/{o[who]}/approve"), headers=_h(s, "manager")), 200)
    n = _wait(base_url, s, "approved", "ORDER_APPROVED")[0]
    assert n["target"] == {"type": "ORDER", "targetId": o["approved"], "eventId": s.event_id}
    assert n["title"] == "티켓 주문이 승인되었습니다!"

    resp = requests.post(_ev(base_url, s, f"/orders/{o['refused']}/refuse"), json={"reasonType": "ETC", "reasonText": "중복 주문"}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    n = _wait(base_url, s, "refused", "ORDER_REFUSED")[0]
    assert n["target"]["targetId"] == o["refused"]
    # v1 앱 주문(결제 채널 없음)이라 환불 계좌 입력 안내는 붙지 않는다 (#728 v2 주문만)
    assert n["body"].endswith("사유: 중복 주문"), n["body"]
    assert n["extra"]["refuseReasonType"] == "ETC" and n["extra"]["refuseReason"] == "중복 주문"

    # v1 거절도 저장 (사유 종류 없음), v1 응답은 그대로
    resp = requests.post(f"{base_url}/v1/events/{s.event_id}/orders/{o['v1refused']}/refuse", json={"reason": "v1 거절"}, headers=_h(s, "master"))
    assert_status(resp, 200)
    assert get_data(resp)["orderUuid"] == o["v1refused"]
    n = _wait(base_url, s, "v1refused", "ORDER_REFUSED")[0]
    assert n["body"].endswith("사유: v1 거절") and "refuseReasonType" not in n["extra"]

    # 승인 후 취소는 거절 알림이 아니라 호스트 취소 알림 (#726)
    _wait(base_url, s, "canceled", "ORDER_APPROVED")
    assert_status(requests.post(_ev(base_url, s, f"/orders/{o['canceled']}/cancel"), json={"reason": "일정 변경"}, headers=_h(s, "manager")), 200)
    _wait(base_url, s, "canceled", "ORDER_CANCELED_BY_HOST")
    time.sleep(1.5)
    assert [n["type"] for n in _list(base_url, s, "canceled")["content"]] == ["ORDER_CANCELED_BY_HOST", "ORDER_APPROVED"]


def test_04_list_paging(base_url, s):
    # 매니저: 주문 4건 + 멤버 추가 1건, 최신 순
    first = _list(base_url, s, "manager", page=0, size=2)
    assert len(first["content"]) == 2 and first["hasNext"] is True
    assert first["totalElements"] is None and first["totalPages"] is None
    all_items = _list(base_url, s, "manager", size=100)["content"]
    assert len(all_items) == 5
    ids = [n["notificationId"] for n in all_items]
    assert ids == sorted(ids, reverse=True)
    assert all_items[-1]["type"] == "HOST_MEMBER_ADDED"
    last = _list(base_url, s, "manager", page=2, size=2)
    assert len(last["content"]) == 1 and last["hasNext"] is False
    assert_status(requests.get(f"{base_url}/v2/me/notifications", params={"size": 101}, headers=_h(s, "manager")), 400)


def test_05_unread_and_read(base_url, s):
    url = f"{base_url}/v2/me/notifications/read"
    assert _unread(base_url, s, "manager") == 5
    mine = [n["notificationId"] for n in _list(base_url, s, "manager", size=100)["content"]]
    others = [n["notificationId"] for n in _list(base_url, s, "master", size=100)["content"]]
    master_unread = _unread(base_url, s, "master")

    # 내 것 2개 + 마스터 것 1개 → 내 것만
    resp = requests.post(url, json={"notificationIds": mine[:2] + others[:1]}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert get_data(resp) == {"updatedCount": 2, "unreadCount": 3}
    assert _unread(base_url, s, "master") == master_unread
    # 같은 id 재요청 → 0건 (멱등)
    resp = requests.post(url, json={"notificationIds": mine[:2]}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert get_data(resp)["updatedCount"] == 0
    read_flags = {n["notificationId"]: n["isRead"] for n in _list(base_url, s, "manager", size=100)["content"]}
    assert read_flags[mine[0]] is True and read_flags[mine[2]] is False

    # 남(외부인)이 매니저 알림 읽음 시도 → 무시
    resp = requests.post(url, json={"notificationIds": mine[2:]}, headers=_h(s, "outsider"))
    assert_status(resp, 200)
    assert get_data(resp)["updatedCount"] == 0
    assert _unread(base_url, s, "manager") == 3

    # 전체 읽음 → 3건, 다시 → 0건
    resp = requests.post(url, json={"readAll": True}, headers=_h(s, "manager"))
    assert get_data(resp) == {"updatedCount": 3, "unreadCount": 0}
    resp = requests.post(url, json={"readAll": True}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert get_data(resp) == {"updatedCount": 0, "unreadCount": 0}
    assert _unread(base_url, s, "master") == master_unread


def test_06_unauthorized(base_url, s):
    assert_status(requests.get(f"{base_url}/v2/me/notifications"), 401)
    assert_status(requests.get(f"{base_url}/v2/me/notifications/unread-count"), 401)
    assert_status(requests.post(f"{base_url}/v2/me/notifications/read", json={"readAll": True}), 401)
