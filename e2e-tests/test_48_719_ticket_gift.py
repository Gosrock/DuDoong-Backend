"""
v2 사용자 앱 티켓탭·선물 E2E 테스트 (#719).

시나리오: 호스트·공연·무료 티켓(즉시 발급) 준비 → 선물 생성·랜딩(viewState)·수락(uuid 교체)·거절·회수·반환·메모 →
v1 경로 보호(입장·티켓 상세·주문 티켓 목록·사용자 환불, 옛 uuid 404) → v2 체크인(GIFT_PENDING·OTHER_EVENT) →
연쇄 취소(v1 호스트 취소, 운영 취소, 운영 사용자 정지, 운영 공연 삭제) → 선물 만료(DB 로 공연 시각 이동) → 알림 →
1인 제한(원 구매자 기준) → MySQL 동시성(같은 링크 동시 수락, 수락 ↔ 회수, 수락 ↔ v1 호스트 취소, 생성 ↔ 사용자 취소).

DB 직접 접근(운영자 권한 부여, 공연 시각 이동)은 E2E_DB(기본 dudoong, test_47 과 같은 환경변수) 의 로컬 MySQL(127.0.0.1:13306, docker-compose 개발용 계정)에 한다.
재실행해도 충돌하지 않도록 유저 이메일에 실행마다 다른 접미사를 붙인다.
"""
import os
import subprocess
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta

import pytest
import requests

from conftest import assert_status, get_data

RUN = uuid.uuid4().hex[:8]
FMT = "%Y.%m.%d %H:%M"
START = (datetime.now() + timedelta(days=30)).replace(hour=18, minute=0, second=0, microsecond=0)
END = START + timedelta(minutes=120)
PLACE = {"name": "롤링홀", "address": "서울 마포구 어울마당로 35", "latitude": 37.548369, "longitude": 126.920036}
SECTIONS = [{"title": "공연 소개", "content": "<p>선물 테스트</p>", "sortOrder": 0}]
DB_NAME = os.environ.get("E2E_DB", "dudoong")
PEOPLE = ["master", "manager", "guest", "sender", "receiver", "other", "admin", "racer1", "racer2", "racer3", "racer4", "racer5", "limit"] + [f"buyer{i}" for i in range(1, 10)]


class GiftState:
    tokens: dict = {}
    emails: dict = {}
    user_ids: dict = {}
    host_id: int = 0
    events: dict = {}
    tickets: dict = {}


@pytest.fixture(scope="module")
def s():
    return GiftState()


def _sql(sql):
    result = subprocess.run(
        ["mysql", "-h", "127.0.0.1", "-P", "13306", "-u", "dudoong", "-pdudoong", DB_NAME, "-N", "-e", sql],
        capture_output=True, text=True,
    )
    assert result.returncode == 0, result.stderr
    return result.stdout.strip()


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
    token = get_data(resp)["accessToken"]
    import base64
    import json
    part = token.split(".")[1]
    part += "=" * (-len(part) % 4)
    return token, int(json.loads(base64.urlsafe_b64decode(part))["sub"])


def _ev(base_url, event_id, path=""):
    return f"{base_url}/v2/events/{event_id}{path}"


def _new_event(base_url, s, key, purchase_limit=4):
    resp = requests.post(
        f"{base_url}/v2/events",
        json={"hostId": s.host_id, "name": f"선물공연{key}", "startAt": START.strftime(FMT), "endAt": END.strftime(FMT), "hasTicket": True},
        headers=_h(s, "manager"),
    )
    assert_status(resp, 200)
    event_id = get_data(resp)["eventId"]
    resp = requests.post(_ev(base_url, event_id, "/ticket-items"), json={
        "payType": "FREE", "name": "무료", "description": "무료", "price": 0, "supplyCount": 100, "account": None,
        "approvalRequired": False, "isQuantityPublic": True, "purchaseLimit": purchase_limit, "saleStartAt": None, "saleEndAt": None,
    }, headers=_h(s, "manager"))
    assert_status(resp, 200)
    ticket_id = get_data(resp)["ticketItemId"]
    key_img = get_data(requests.post(_ev(base_url, event_id, "/images"), json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "manager")))["key"]
    assert_status(requests.patch(_ev(base_url, event_id, "/basic"), json={"posterImageKey": key_img, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]}, headers=_h(s, "manager")), 200)
    assert_status(requests.put(_ev(base_url, event_id, "/sections"), json=SECTIONS, headers=_h(s, "manager")), 200)
    assert_status(requests.post(_ev(base_url, event_id, "/open"), headers=_h(s, "manager")), 200)
    s.events[key], s.tickets[key] = event_id, ticket_id
    return event_id


def _buy(base_url, s, who, key, quantity=1):
    """무료 즉시 발급 주문 → (orderUuid, 티켓 uuid 목록)"""
    resp = requests.post(f"{base_url}/v2/orders", json={
        "eventId": s.events[key], "ticketItemId": s.tickets[key], "quantity": quantity,
        "options": {"applyToAll": True, "answers": []}, "perTicketOptions": None,
        "paymentMethod": "FREE", "depositorName": None, "agreeRefundPolicy": True,
    }, headers=_h(s, who))
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["status"] == "APPROVED"
    return data["orderUuid"], [t["ticketUuid"] for t in data["issuedTickets"]]


def _gift(base_url, s, who, ticket_uuid, memo=None):
    return requests.post(f"{base_url}/v2/me/tickets/{ticket_uuid}/gift", json={"memo": memo}, headers=_h(s, who))


def _gift_ok(base_url, s, who, ticket_uuid, memo=None):
    resp = _gift(base_url, s, who, ticket_uuid, memo)
    assert_status(resp, 200)
    return get_data(resp)


def _accept(base_url, s, who, token):
    return requests.post(f"{base_url}/v2/gifts/{token}/accept", headers=_h(s, who))


def _landing(base_url, s, who, token):
    resp = requests.get(f"{base_url}/v2/gifts/{token}", headers=_h(s, who) if who else {})
    assert_status(resp, 200)
    return get_data(resp)


def _cancel_gift(base_url, s, who, gift_id):
    return requests.delete(f"{base_url}/v2/me/gifts/{gift_id}", headers=_h(s, who))


def _sent(base_url, s, who):
    resp = requests.get(f"{base_url}/v2/me/gifts", params={"direction": "SENT", "size": 50}, headers=_h(s, who))
    assert_status(resp, 200)
    return {g["giftId"]: g for g in get_data(resp)["content"]}


def _my_tickets(base_url, s, who):
    resp = requests.get(f"{base_url}/v2/me/tickets", headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)["groups"]


def _ticket(base_url, s, who, ticket_uuid):
    return requests.get(f"{base_url}/v2/me/tickets/{ticket_uuid}", headers=_h(s, who))


def _wait_notification(base_url, s, who, type_, target):
    deadline = time.time() + 10
    while time.time() < deadline:
        resp = requests.get(f"{base_url}/v2/me/notifications", params={"size": 100}, headers=_h(s, who))
        assert_status(resp, 200)
        found = [n for n in get_data(resp)["content"] if n["type"] == type_ and n["target"]["id"] == str(target)]
        if found:
            return found
        time.sleep(0.2)
    return []


def _admin_base(base_url):
    return base_url.replace("/api", "/internal-api")


def test_01_setup(base_url, s):
    for who in PEOPLE:
        email = f"gift719-{who}-{RUN}@dudoong.com"
        s.tokens[who], s.user_ids[who] = _login(base_url, email, f"선물{who}")
        s.emails[who] = email
    _sql(f"UPDATE tbl_user SET account_role='ADMIN' WHERE user_id={s.user_ids['admin']}")
    s.tokens["admin"], _ = _login(base_url, s.emails["admin"], "선물admin")

    resp = requests.post(f"{base_url}/v2/hosts", json={"name": f"선물{RUN[:5]}", "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    s.host_id = get_data(resp)["hostId"]
    resp = requests.post(
        f"{base_url}/v2/hosts/{s.host_id}/members",
        json={"members": [{"email": s.emails["manager"], "role": "MANAGER"}, {"email": s.emails["guest"], "role": "GUEST"}]},
        headers=_h(s, "master"),
    )
    assert_status(resp, 200)
    for key in ("main", "cascade", "admin", "expire", "race", "removed"):
        _new_event(base_url, s, key)
    _new_event(base_url, s, "limit", purchase_limit=2)


def test_02_create_landing_accept_uuid_swap(base_url, s):
    order_uuid, uuids = _buy(base_url, s, "sender", "main", 2)
    created = _gift_ok(base_url, s, "sender", uuids[0], " 동생 ")
    token = created["giftToken"]
    assert len(token) == 43 and created["linkPath"] == f"/gifts/{token}" and created["memo"] == "동생"
    assert _code(_gift(base_url, s, "sender", uuids[0])) == "Gift_400_2"
    assert _code(_gift(base_url, s, "other", uuids[1])) == "IssuedTicket_404_1"

    anon = _landing(base_url, s, None, token)
    assert anon["viewState"] == "AVAILABLE" and anon["isLoggedIn"] is False and anon["senderName"] == "선물sender"
    assert anon["event"]["placeName"] == "롤링홀" and "동생" not in str(anon)
    assert _landing(base_url, s, "sender", token)["viewState"] == "OWN_LINK"
    assert _code(_accept(base_url, s, "sender", token)) == "Gift_400_4"

    groups = {g["orderUuid"]: g for g in _my_tickets(base_url, s, "sender")}
    assert groups[order_uuid]["counts"] == {"GIFT_PENDING": 1, "APPROVED": 1}

    resp = _accept(base_url, s, "receiver", token)
    assert_status(resp, 200)
    new_uuid = get_data(resp)["ticketUuid"]
    assert new_uuid != uuids[0]
    s.main = {"order": order_uuid, "old": uuids[0], "new": new_uuid, "other": uuids[1], "giftId": created["giftId"], "token": token}
    assert _code(_accept(base_url, s, "other", token)) == "Gift_400_3"

    detail = get_data(_ticket(base_url, s, "receiver", new_uuid))
    assert detail["qrValue"] == new_uuid and detail["giftState"] == "RECEIVED" and detail["orderUuid"] is None
    assert _code(_ticket(base_url, s, "sender", uuids[0])) == "IssuedTicket_404_1"
    landing = _landing(base_url, s, "receiver", token)
    assert landing["viewState"] == "ALREADY_ACCEPTED" and landing["isReceiver"] is True and landing["event"] is None
    assert _landing(base_url, s, "other", token)["isReceiver"] is False

    # 보낸 사람 T-1·O-3: 선물 완료 행 (uuid 없음)
    groups = {g["orderUuid"]: g for g in _my_tickets(base_url, s, "sender")}
    rows = groups[order_uuid]["tickets"]
    assert [r["state"] for r in rows] == ["GIFT_SENT", "APPROVED"] and rows[0]["ticketUuid"] is None
    resp = requests.get(f"{base_url}/v2/me/orders/{order_uuid}", headers=_h(s, "sender"))
    issued = get_data(resp)["issuedTickets"]
    assert [t["giftState"] for t in issued] == ["SENT", "NONE"] and issued[0]["ticketUuid"] is None


def test_03_v1_paths_after_accept(base_url, s):
    m = s.main
    ev = s.events["main"]
    # 옛 uuid: v1 입장·상세 404, v2 스캔 OTHER_EVENT
    resp = requests.patch(f"{base_url}/v1/events/{ev}/issuedTickets/{m['old']}", headers=_h(s, "manager"))
    assert resp.status_code == 404 and _code(resp) == "IssuedTicket_404_1"
    resp = requests.get(f"{base_url}/v1/issuedTickets/{m['old']}", headers=_h(s, "sender"))
    assert resp.status_code == 404 and _code(resp) == "IssuedTicket_404_1"
    resp = requests.post(_ev(base_url, ev, "/check-ins"), json={"ticketUuid": m["old"]}, headers=_h(s, "guest"))
    assert get_data(resp)["result"] == "OTHER_EVENT"
    # v1 주문 티켓 목록: 주문자가 지금 가진 티켓만
    resp = requests.get(f"{base_url}/v1/orders/{m['order']}/tickets", headers=_h(s, "sender"))
    assert [t["uuid"] for t in get_data(resp)["tickets"]] == [m["other"]]
    # 선물 완료 티켓이 있으면 v1 사용자 환불·v2 취소 모두 Order_400_24
    resp = requests.post(f"{base_url}/v1/orders/{m['order']}/refund", headers=_h(s, "sender"))
    assert resp.status_code == 400 and _code(resp) == "Order_400_24"
    resp = requests.post(f"{base_url}/v2/me/orders/{m['order']}/cancel", json={"refundAccount": None}, headers=_h(s, "sender"))
    assert _code(resp) == "Order_400_24"
    # 받은 사람 새 uuid 는 v1 상세 가능
    assert_status(requests.get(f"{base_url}/v1/issuedTickets/{m['new']}", headers=_h(s, "receiver")), 200)


def test_04_pending_blocks_all_entrance_paths(base_url, s):
    m = s.main
    ev = s.events["main"]
    created = _gift_ok(base_url, s, "sender", m["other"])
    resp = requests.patch(f"{base_url}/v1/events/{ev}/issuedTickets/{m['other']}", headers=_h(s, "manager"))
    assert resp.status_code == 400 and _code(resp) == "IssuedTicket_400_8"
    resp = requests.get(f"{base_url}/v1/issuedTickets/{m['other']}", headers=_h(s, "sender"))
    assert _code(resp) == "IssuedTicket_400_8"
    resp = requests.post(_ev(base_url, ev, "/check-ins"), json={"ticketUuid": m["other"]}, headers=_h(s, "guest"))
    assert get_data(resp)["result"] == "GIFT_PENDING"
    token = get_data(requests.get(_ev(base_url, ev, "/check-in-qr"), headers=_h(s, "guest")))["token"]
    resp = requests.post(f"{base_url}/v2/check-ins/self", json={"token": token, "ticketUuid": None}, headers=_h(s, "sender"))
    assert get_data(resp)["result"] == "GIFT_PENDING"
    resp = requests.get(f"{base_url}/v1/orders/{m['order']}/tickets", headers=_h(s, "sender"))
    assert [t["uuid"] for t in get_data(resp)["tickets"]] == [None]
    assert get_data(_ticket(base_url, s, "sender", m["other"]))["qrValue"] is None

    # 거절 → 다시 입장 가능 (uuid 유지)
    assert_status(requests.post(f"{base_url}/v2/gifts/{created['giftToken']}/reject", headers=_h(s, "other")), 200)
    assert _landing(base_url, s, None, created["giftToken"])["viewState"] == "REJECTED"
    assert get_data(_ticket(base_url, s, "sender", m["other"]))["qrValue"] == m["other"]
    assert len(_wait_notification(base_url, s, "sender", "GIFT_REJECTED", created["giftId"])) == 1

    # 메모 수정·회수 (회수는 알림 없음)
    again = _gift_ok(base_url, s, "sender", m["other"], "처음")
    resp = requests.patch(f"{base_url}/v2/me/gifts/{again['giftId']}", json={"memo": "친구"}, headers=_h(s, "sender"))
    assert get_data(resp)["memo"] == "친구"
    assert _code(requests.patch(f"{base_url}/v2/me/gifts/{again['giftId']}", json={"memo": "가" * 51}, headers=_h(s, "sender"))) == "Gift_400_9"
    assert _code(_cancel_gift(base_url, s, "other", again["giftId"])) == "Gift_404_1"
    resp = _cancel_gift(base_url, s, "sender", again["giftId"])
    assert get_data(resp)["status"] == "CANCELED" and get_data(resp)["cancelReason"] == "SENDER"
    assert _landing(base_url, s, None, again["giftToken"])["viewState"] == "CANCELED"
    resp = requests.post(_ev(base_url, ev, "/check-ins"), json={"ticketUuid": m["other"]}, headers=_h(s, "guest"))
    assert get_data(resp)["result"] == "ENTERED"


def test_05_return_and_notifications(base_url, s):
    m = s.main
    assert len(_wait_notification(base_url, s, "sender", "GIFT_SENT", m["giftId"])) == 1
    assert len(_wait_notification(base_url, s, "sender", "GIFT_ACCEPTED", m["giftId"])) == 1
    assert len(_wait_notification(base_url, s, "receiver", "GIFT_RECEIVED", m["giftId"])) == 1
    assert _code(requests.post(f"{base_url}/v2/me/tickets/{m['new']}/gift", json={}, headers=_h(s, "receiver"))) == "Gift_400_1"
    resp = requests.post(f"{base_url}/v2/me/tickets/{m['new']}/return", headers=_h(s, "receiver"))
    assert_status(resp, 200)
    assert get_data(resp)["status"] == "RETURNED"
    assert _code(_ticket(base_url, s, "receiver", m["new"])) == "IssuedTicket_404_1"
    assert len(_wait_notification(base_url, s, "sender", "GIFT_RETURNED", m["giftId"])) == 1
    back = _my_tickets(base_url, s, "sender")
    rows = next(g for g in back if g["orderUuid"] == m["order"])["tickets"]
    returned_uuid = rows[0]["ticketUuid"]
    assert returned_uuid not in (m["old"], m["new"]) and rows[0]["giftState"] == "NONE"
    # 반환된 티켓은 다시 선물 가능
    _gift_ok(base_url, s, "sender", returned_uuid)
    # 회수 알림은 없음
    resp = requests.get(f"{base_url}/v2/me/notifications", params={"size": 100}, headers=_h(s, "sender"))
    assert all(n["type"] != "GIFT_CANCELED" for n in get_data(resp)["content"])


def test_06_v1_host_cancel_cascade(base_url, s):
    ev = s.events["cascade"]
    order_uuid, uuids = _buy(base_url, s, "sender", "cascade", 2)
    pending = _gift_ok(base_url, s, "sender", uuids[0])
    accepted = _gift_ok(base_url, s, "sender", uuids[1])
    new_uuid = get_data(_accept(base_url, s, "receiver", accepted["giftToken"]))["ticketUuid"]
    resp = requests.post(f"{base_url}/v1/events/{ev}/orders/{order_uuid}/cancel", json={"reason": "공연 취소"}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    sent = _sent(base_url, s, "sender")
    assert sent[pending["giftId"]]["status"] == "CANCELED" and sent[pending["giftId"]]["cancelReason"] == "ORDER_CANCELED"
    assert sent[accepted["giftId"]]["status"] == "ACCEPTED"
    assert _landing(base_url, s, None, pending["giftToken"])["viewState"] == "CANCELED"
    assert get_data(_ticket(base_url, s, "receiver", new_uuid))["state"] == "CANCELED"
    assert len(_wait_notification(base_url, s, "receiver", "GIFT_TICKET_CANCELED", accepted["giftId"])) == 1
    assert _code(_accept(base_url, s, "other", pending["giftToken"])) == "Gift_400_3"


def test_07_admin_paths(base_url, s):
    admin = _admin_base(base_url)
    # 운영 취소: 대기 선물 무효 + 선물 완료 티켓 취소 + 받은 사람 알림 (주문자에게는 #726 호스트 취소 알림, 서로 겹치지 않음)
    order_uuid, uuids = _buy(base_url, s, "sender", "admin", 2)
    pending = _gift_ok(base_url, s, "sender", uuids[0])
    accepted = _gift_ok(base_url, s, "sender", uuids[1])
    receiver_uuid = get_data(_accept(base_url, s, "receiver", accepted["giftToken"]))["ticketUuid"]
    resp = requests.post(f"{admin}/v1/orders/{order_uuid}/cancel", json={"reason": "운영"}, headers=_h(s, "admin"))
    assert resp.status_code in (200, 204), resp.text
    sent = _sent(base_url, s, "sender")
    assert sent[pending["giftId"]]["cancelReason"] == "ORDER_CANCELED"
    assert sent[accepted["giftId"]]["status"] == "ACCEPTED"
    assert get_data(_ticket(base_url, s, "receiver", receiver_uuid))["state"] == "CANCELED"
    assert len(_wait_notification(base_url, s, "receiver", "GIFT_TICKET_CANCELED", accepted["giftId"])) == 1
    assert len(_wait_notification(base_url, s, "sender", "ORDER_CANCELED_BY_HOST", order_uuid)) == 1
    resp = requests.get(f"{base_url}/v2/me/notifications", params={"size": 100}, headers=_h(s, "receiver"))
    assert not [n for n in get_data(resp)["content"] if n["type"] == "ORDER_CANCELED_BY_HOST" and n["target"]["id"] == order_uuid]
    resp = requests.get(f"{base_url}/v2/me/notifications", params={"size": 100}, headers=_h(s, "sender"))
    assert not [n for n in get_data(resp)["content"] if n["type"] == "GIFT_TICKET_CANCELED"]

    # 운영 환불 완료·환불 상태 변경: 이미 철회된 주문이라 선물 상태는 그대로, 오류 없음, 받은 사람 취소 알림 중복 없음
    resp = requests.patch(f"{admin}/v1/refunds/{order_uuid}/complete", headers=_h(s, "admin"))
    assert resp.status_code in (200, 204), resp.text
    resp = requests.patch(f"{admin}/v1/orders/{order_uuid}/refund-status", json={"refundStatus": "REFUND_COMPLETED"}, headers=_h(s, "admin"))
    assert resp.status_code in (200, 204), resp.text
    sent = _sent(base_url, s, "sender")
    assert sent[pending["giftId"]]["status"] == "CANCELED" and sent[accepted["giftId"]]["status"] == "ACCEPTED"
    time.sleep(1)
    assert len(_wait_notification(base_url, s, "receiver", "GIFT_TICKET_CANCELED", accepted["giftId"])) == 1

    # 운영 환불 완료를 철회 전 주문에 바로 찍어도(v1·운영은 허용) 티켓이 유효하므로 대기 선물은 그대로
    order2, uuids2 = _buy(base_url, s, "sender", "admin", 1)
    g3 = _gift_ok(base_url, s, "sender", uuids2[0])
    resp = requests.patch(f"{admin}/v1/refunds/{order2}/complete", headers=_h(s, "admin"))
    assert resp.status_code in (200, 204), resp.text
    assert _sent(base_url, s, "sender")[g3["giftId"]]["status"] == "PENDING"
    assert_status(_cancel_gift(base_url, s, "sender", g3["giftId"]), 200)

    # 운영 사용자 정지 → 보낸 대기 선물 취소
    _, uuids = _buy(base_url, s, "other", "admin", 1)
    g = _gift_ok(base_url, s, "other", uuids[0])
    assert requests.patch(f"{admin}/v1/users/{s.user_ids['other']}/status", json={"status": "SUSPENDED"}, headers=_h(s, "admin")).status_code in (200, 204)
    assert _landing(base_url, s, None, g["giftToken"])["viewState"] == "CANCELED"
    assert requests.patch(f"{admin}/v1/users/{s.user_ids['other']}/status", json={"status": "NORMAL"}, headers=_h(s, "admin")).status_code in (200, 204)
    s.tokens["other"], _ = _login(base_url, s.emails["other"], "선물other")
    assert _sent(base_url, s, "other")[g["giftId"]]["cancelReason"] == "SENDER_WITHDRAWN"

    # 운영 공연 삭제 → 대기 선물 취소
    _, uuids = _buy(base_url, s, "sender", "removed", 1)
    g2 = _gift_ok(base_url, s, "sender", uuids[0])
    assert requests.delete(f"{admin}/v1/events/{s.events['removed']}", headers=_h(s, "admin")).status_code in (200, 204)
    assert _sent(base_url, s, "sender")[g2["giftId"]]["cancelReason"] == "EVENT_REMOVED"


def test_08_expired(base_url, s):
    ev = s.events["expire"]
    _, uuids = _buy(base_url, s, "sender", "expire", 2)
    g = _gift_ok(base_url, s, "sender", uuids[0])
    accepted = _gift_ok(base_url, s, "sender", uuids[1])
    receiver_uuid = get_data(_accept(base_url, s, "receiver", accepted["giftToken"]))["ticketUuid"]
    # 시작 후 종료 전: 수락은 되지만 반환은 안 됨
    _sql(f"UPDATE tbl_event SET start_at = NOW() - INTERVAL 30 MINUTE WHERE event_id = {ev}")
    assert _code(requests.post(f"{base_url}/v2/me/tickets/{receiver_uuid}/return", headers=_h(s, "receiver"))) == "Gift_400_7"
    assert _landing(base_url, s, "other", g["giftToken"])["viewState"] == "AVAILABLE"
    # 종료(시작 + 120분 경과) → 선물 만료
    _sql(f"UPDATE tbl_event SET start_at = NOW() - INTERVAL 3 HOUR WHERE event_id = {ev}")
    assert _landing(base_url, s, "sender", g["giftToken"])["viewState"] == "EXPIRED"
    assert _code(_accept(base_url, s, "other", g["giftToken"])) == "Gift_400_5"
    groups = _my_tickets(base_url, s, "sender")
    row = next(t for grp in groups for t in grp["tickets"] if t.get("giftId") == g["giftId"])
    assert row["state"] == "GIFT_EXPIRED" and row["isGiftExpired"] is True
    assert_status(_cancel_gift(base_url, s, "sender", g["giftId"]), 200)


def test_09_purchase_limit_by_buyer(base_url, s):
    """1인 2장 티켓: 보낸 사람 2장 구매·1장 선물 → 더 못 산다 / 받은 사람은 받은 1장과 무관하게 2장 산다 (원 구매자 기준)"""
    _, uuids = _buy(base_url, s, "limit", "limit", 2)
    g = _gift_ok(base_url, s, "limit", uuids[0])
    assert_status(_accept(base_url, s, "receiver", g["giftToken"]), 200)
    body = {
        "eventId": s.events["limit"], "ticketItemId": s.tickets["limit"], "quantity": 1,
        "options": {"applyToAll": True, "answers": []}, "perTicketOptions": None,
        "paymentMethod": "FREE", "depositorName": None, "agreeRefundPolicy": True,
    }
    resp = requests.post(f"{base_url}/v2/orders", json=body, headers=_h(s, "limit"))
    assert resp.status_code == 400, resp.text
    # v1 장바구니도 같은 기준
    resp = requests.post(f"{base_url}/v1/carts", json={"items": [{"itemId": s.tickets["limit"], "quantity": 1, "options": []}]}, headers=_h(s, "limit"))
    assert resp.status_code == 400, resp.text
    resp = requests.post(f"{base_url}/v1/carts", json={"items": [{"itemId": s.tickets["limit"], "quantity": 2, "options": []}]}, headers=_h(s, "receiver"))
    assert_status(resp, 200)
    _buy(base_url, s, "receiver", "limit", 2)


def _race(*calls):
    with ThreadPoolExecutor(max_workers=len(calls)) as pool:
        return [f.result() for f in [pool.submit(c) for c in calls]]


def test_10_concurrency_same_link(base_url, s):
    """같은 링크 동시 수락 5명 → 1명만 성공, 나머지 Gift_400_3, 티켓 소유자 1명"""
    _, uuids = _buy(base_url, s, "sender", "race", 1)
    g = _gift_ok(base_url, s, "sender", uuids[0])
    racers = ["racer1", "racer2", "racer3", "racer4", "racer5"]
    results = _race(*[(lambda w=w: _accept(base_url, s, w, g["giftToken"])) for w in racers])
    ok = [r for r in results if r.status_code == 200]
    assert len(ok) == 1, [r.text for r in results]
    assert all(_code(r) == "Gift_400_3" for r in results if r.status_code != 200)
    owner_id = _sql(f"SELECT user_id FROM tbl_issued_ticket WHERE uuid = '{get_data(ok[0])['ticketUuid']}'")
    assert int(owner_id) in [s.user_ids[w] for w in racers]
    assert _sql(f"SELECT COUNT(*) FROM tbl_ticket_gift WHERE ticket_gift_id = {g['giftId']} AND status = 'ACCEPTED'") == "1"


def test_11_concurrency_accept_vs_cancel(base_url, s):
    """수락 ↔ 회수 동시: 정확히 한쪽만 성공, 상태와 소유자가 맞는다"""
    # 같은 사용자의 같은 주문은 10초 안 중복 요청으로 앞 주문을 돌려주므로 회차마다 구매자를 바꾼다
    for buyer in ("buyer1", "buyer2", "buyer3"):
        _, uuids = _buy(base_url, s, buyer, "race", 1)
        g = _gift_ok(base_url, s, buyer, uuids[0])
        a, c = _race(lambda: _accept(base_url, s, "racer1", g["giftToken"]), lambda: _cancel_gift(base_url, s, buyer, g["giftId"]))
        assert [a.status_code, c.status_code].count(200) == 1, (a.text, c.text)
        status, owner = _sql(f"SELECT g.status, t.user_id FROM tbl_ticket_gift g JOIN tbl_issued_ticket t ON t.issued_ticket_id = g.issued_ticket_id WHERE g.ticket_gift_id = {g['giftId']}").split()
        if a.status_code == 200:
            assert status == "ACCEPTED" and int(owner) == s.user_ids["racer1"] and _code(c) == "Gift_400_3"
        else:
            assert status == "CANCELED" and int(owner) == s.user_ids[buyer] and _code(a) == "Gift_400_3"


def test_12_concurrency_accept_vs_v1_host_cancel(base_url, s):
    """수락 ↔ v1 호스트 취소 동시: 어느 쪽이 먼저든 주문 취소 + 티켓 취소, 선물은 (ACCEPTED + 받은 사람 소유) 또는 (CANCELED(ORDER_CANCELED) + 보낸 사람 소유)"""
    ev = s.events["race"]
    for buyer in ("buyer4", "buyer5", "buyer6"):
        order_uuid, uuids = _buy(base_url, s, buyer, "race", 1)
        g = _gift_ok(base_url, s, buyer, uuids[0])
        a, c = _race(
            lambda: _accept(base_url, s, "racer2", g["giftToken"]),
            lambda: requests.post(f"{base_url}/v1/events/{ev}/orders/{order_uuid}/cancel", json={"reason": "경합"}, headers=_h(s, "manager")),
        )
        assert c.status_code == 200, c.text
        row = _sql(
            "SELECT g.status, IFNULL(g.cancel_reason,'-'), t.user_id, t.issued_ticket_status, o.order_status FROM tbl_ticket_gift g "
            "JOIN tbl_issued_ticket t ON t.issued_ticket_id = g.issued_ticket_id JOIN tbl_order o ON o.uuid = t.order_uuid "
            f"WHERE g.ticket_gift_id = {g['giftId']}"
        ).split()
        status, reason, owner, ticket_status, order_status = row
        assert order_status == "CANCELED" and ticket_status == "CANCELED"
        if a.status_code == 200:
            assert status == "ACCEPTED" and int(owner) == s.user_ids["racer2"]
        else:
            assert _code(a) == "Gift_400_3" and status == "CANCELED" and reason == "ORDER_CANCELED" and int(owner) == s.user_ids[buyer]


def test_13_concurrency_create_vs_user_cancel(base_url, s):
    """선물 생성 ↔ 사용자 취소(O-4) 동시: 둘 다 성공하는 일은 없다 (주문 락으로 줄 섬)"""
    for buyer in ("buyer7", "buyer8", "buyer9"):
        order_uuid, uuids = _buy(base_url, s, buyer, "race", 1)
        gift_resp, cancel_resp = _race(
            lambda: _gift(base_url, s, buyer, uuids[0]),
            lambda: requests.post(f"{base_url}/v2/me/orders/{order_uuid}/cancel", json={"refundAccount": None}, headers=_h(s, buyer)),
        )
        assert [gift_resp.status_code, cancel_resp.status_code].count(200) == 1, (gift_resp.text, cancel_resp.text)
        pending = _sql(f"SELECT COUNT(*) FROM tbl_ticket_gift g JOIN tbl_issued_ticket t ON t.issued_ticket_id = g.issued_ticket_id WHERE t.uuid = '{uuids[0]}' AND g.status = 'PENDING'")
        order_status = _sql(f"SELECT order_status FROM tbl_order WHERE uuid = '{order_uuid}'")
        if gift_resp.status_code == 200:
            assert pending == "1" and order_status == "APPROVED" and _code(cancel_resp) == "Order_400_24"
        else:
            assert pending == "0" and order_status == "REFUND" and _code(gift_resp) == "Gift_400_1"
