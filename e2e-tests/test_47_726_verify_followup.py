"""
#726 v2 삼자 검증 후속 E2E (MySQL).

시나리오: 호스트(마스터·매니저·일반)·공연(장소)·두둥티켓 A(재고 5)·B(재고 20, 1인 4장)·무료 승인 티켓 준비 → v2 등록 →
H-14 장소 / E-1 myRole → O-0 결제 화면 계좌(로그인만, 공개 P-5 에는 없음) →
P-5 잔여·매진(승인 대기 차감, 주문 재고 검사와 같은 기준) → 입금자명 검색·엑셀 열(수식 방어) →
승인형 1인 제한 동시 주문(같은 사용자 2장 + 3장 > 4 → 1건만) → 호스트 취소·환불 완료 사용자 알림(v1·v2 경로).

재실행해도 충돌하지 않도록 유저 이메일에 실행마다 다른 접미사를 붙인다.
DB 직접 접근은 운영 어드민 경로 검증(test_08)에서 사용자 역할을 ADMIN 으로 바꿀 때만 한다 — 접속 정보는 conftest 의 e2e_db fixture(환경변수 E2E_DB 등, #737).
"""
import io
import re
import time
import uuid
import zipfile
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta
from xml.etree import ElementTree

import pytest
import requests

from conftest import assert_status, get_data

RUN = uuid.uuid4().hex[:8]
FMT = "%Y.%m.%d %H:%M"
START = (datetime.now() + timedelta(days=30)).replace(hour=18, minute=0, second=0, microsecond=0)
END = START + timedelta(minutes=120)
PLACE = {"name": "롤링홀", "address": "서울 마포구 어울마당로 35", "latitude": 37.548369, "longitude": 126.920036}
ACCOUNT = {"bank": "신한은행", "holder": "고스락", "number": "110-123-456789"}
BUYERS = ["b1", "b2", "b3", "late", "evil", "lim1", "lim2", "lim3", "cancel1", "cancel2", "free1", "adm1", "adm2", "admin", "fcfs_v2", "fcfs_v1", "fcfs_self", "wrong"]


class FollowupState:
    tokens: dict = {}
    host_id: int = 0
    event_id: int = 0
    a: int = 0
    b: int = 0
    free: int = 0
    fcfs: int = 0
    orders: dict = {}


@pytest.fixture(scope="module")
def s():
    return FollowupState()


def _h(s, who):
    return {"Authorization": f"Bearer {s.tokens[who]}"}


def _ev(base_url, s, path):
    return f"{base_url}/v2/events/{s.event_id}{path}"


def _login(base_url, email, name):
    resp = requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={"email": email, "name": name, "phoneNumber": "010-0000-0000", "profileImage": None, "marketingAgree": False},
    )
    assert_status(resp, 200)
    return get_data(resp)["accessToken"]


def _email(who):
    return f"fix726-{who}-{RUN}@dudoong.com"


def _ticket_body(name, supply, pay_type="DUDOONG", approval=True):
    body = {"payType": pay_type, "name": name, "description": "726", "price": 5000, "supplyCount": supply, "account": ACCOUNT,
            "approvalRequired": approval, "isQuantityPublic": True, "purchaseLimit": 4, "saleStartAt": None, "saleEndAt": None}
    if pay_type == "FREE":
        body.update(price=0, account=None)
    return body


def _v2_order(base_url, s, who, item_id, quantity, depositor="입금자", method="BANK_TRANSFER"):
    body = {
        "eventId": s.event_id, "ticketItemId": item_id, "quantity": quantity,
        "options": {"applyToAll": True, "answers": []}, "perTicketOptions": None,
        "paymentMethod": method, "depositorName": depositor, "agreeRefundPolicy": True,
    }
    return requests.post(f"{base_url}/v2/orders", json=body, headers=_h(s, who))


def _ok_order(base_url, s, who, item_id, quantity, **kw):
    resp = _v2_order(base_url, s, who, item_id, quantity, **kw)
    assert_status(resp, 200)
    return get_data(resp)["orderUuid"]


def _public_item(base_url, s, item_id):
    resp = requests.get(_ev(base_url, s, "/ticket-items"))
    assert_status(resp, 200)
    return next(t for t in get_data(resp) if t["ticketItemId"] == item_id)


def _checkout(base_url, s, who, item_id):
    return requests.get(_ev(base_url, s, f"/ticket-items/{item_id}/checkout"), headers=_h(s, who) if who else {})


def _orders(base_url, s, **params):
    resp = requests.get(_ev(base_url, s, "/orders"), params=params, headers=_h(s, "guest"))
    assert_status(resp, 200)
    return get_data(resp)


def _xlsx_rows(content):
    """첫 시트의 행 목록 (각 행은 셀 문자열 리스트, 빈 셀은 ''). test_42 와 같은 방식"""
    with zipfile.ZipFile(io.BytesIO(content)) as z:
        ns = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
        shared = []
        if "xl/sharedStrings.xml" in z.namelist():
            root = ElementTree.fromstring(z.read("xl/sharedStrings.xml"))
            shared = ["".join(t.text or "" for t in si.iter(f"{{{ns['m']}}}t")) for si in root.findall("m:si", ns)]
        sheet = ElementTree.fromstring(z.read("xl/worksheets/sheet1.xml"))
        rows = []
        for row in sheet.iter(f"{{{ns['m']}}}row"):
            cells = []
            for c in row.findall("m:c", ns):
                col = re.match(r"[A-Z]+", c.get("r")).group(0)
                index = 0
                for ch in col:
                    index = index * 26 + (ord(ch) - 64)
                while len(cells) < index - 1:
                    cells.append("")
                v = c.find("m:v", ns)
                is_ = c.find("m:is", ns)
                if is_ is not None:
                    value = "".join(t.text or "" for t in is_.iter(f"{{{ns['m']}}}t"))
                else:
                    value = "" if v is None else v.text
                    value = shared[int(value)] if c.get("t") == "s" and value != "" else value
                cells.append(value)
            rows.append(cells)
        return rows


def _wait_notification(base_url, s, who, type_, target, timeout=10):
    deadline = time.time() + timeout
    while time.time() < deadline:
        resp = requests.get(f"{base_url}/v2/me/notifications", params={"size": 100}, headers=_h(s, who))
        assert_status(resp, 200)
        found = [n for n in get_data(resp)["content"] if n["type"] == type_ and n["target"]["id"] == target]
        if found:
            return found
        time.sleep(0.2)
    return []


def _notifications(base_url, s, who, type_):
    resp = requests.get(f"{base_url}/v2/me/notifications", params={"size": 100}, headers=_h(s, who))
    assert_status(resp, 200)
    return [n for n in get_data(resp)["content"] if n["type"] == type_]


def test_01_setup(base_url, s):
    for who in ["master", "manager", "guest"] + BUYERS:
        s.tokens[who] = _login(base_url, _email(who), f"후속{who}")
    resp = requests.post(f"{base_url}/v2/hosts", json={"name": f"후속{RUN[:5]}", "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    s.host_id = get_data(resp)["hostId"]
    members = [{"email": _email("manager"), "role": "MANAGER"}, {"email": _email("guest"), "role": "GUEST"}]
    assert_status(requests.post(f"{base_url}/v2/hosts/{s.host_id}/members", json={"members": members}, headers=_h(s, "master")), 200)
    resp = requests.post(
        f"{base_url}/v2/events",
        json={"hostId": s.host_id, "name": "726후속공연", "startAt": START.strftime(FMT), "endAt": END.strftime(FMT), "hasTicket": True},
        headers=_h(s, "manager"),
    )
    assert_status(resp, 200)
    s.event_id = get_data(resp)["eventId"]
    for attr, name, supply, pay_type, approval in [("a", "A석", 5, "DUDOONG", True), ("b", "B석", 30, "DUDOONG", True), ("free", "무료", 10, "FREE", True), ("fcfs", "선착순", 10, "FREE", False)]:
        resp = requests.post(_ev(base_url, s, "/ticket-items"), json=_ticket_body(name, supply, pay_type, approval), headers=_h(s, "manager"))
        assert_status(resp, 200)
        setattr(s, attr, get_data(resp)["ticketItemId"])
    key = get_data(requests.post(_ev(base_url, s, "/images"), json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "manager")))["key"]
    resp = requests.patch(_ev(base_url, s, "/basic"), json={"posterImageKey": key, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert_status(requests.put(_ev(base_url, s, "/sections"), json=[{"title": "소개", "content": "<p>726</p>", "sortOrder": 0}], headers=_h(s, "manager")), 200)
    assert_status(requests.post(_ev(base_url, s, "/open"), headers=_h(s, "manager")), 200)


def test_02_host_events_place_and_my_role(base_url, s):
    # H-14: 공개 상세(P-3)와 같은 place
    resp = requests.get(f"{base_url}/v2/hosts/{s.host_id}/events")
    assert_status(resp, 200)
    card = next(e for e in get_data(resp)["content"] if e["eventId"] == s.event_id)
    detail = get_data(requests.get(_ev(base_url, s, "")))
    assert card["place"] == detail["place"]
    assert card["place"]["name"] == "롤링홀" and card["place"]["address"] == PLACE["address"]
    # E-1: 보는 사람마다 내 역할
    for who, role in [("master", "MASTER"), ("manager", "MANAGER"), ("guest", "GUEST")]:
        resp = requests.get(f"{base_url}/v2/me/events", params={"keyword": "726후속"}, headers=_h(s, who))
        assert_status(resp, 200)
        item = next(e for e in get_data(resp)["content"] if e["eventId"] == s.event_id)
        assert item["myRole"] == role, (who, item)


def test_03_checkout_account_login_only(base_url, s):
    assert_status(_checkout(base_url, s, None, s.a), 401)
    resp = _checkout(base_url, s, "b1", s.a)
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["account"] == {"bankName": "신한은행", "accountHolder": "고스락", "accountNumber": "110-123-456789"}
    assert data["ticket"] == _public_item(base_url, s, s.a)
    free = get_data(_checkout(base_url, s, "b1", s.free))
    assert free["account"] is None and free["ticket"]["payType"] == "FREE"
    # 공개 P-5 에는 계좌가 없다
    raw = requests.get(_ev(base_url, s, "/ticket-items")).text
    assert "110-123-456789" not in raw and "account" not in raw
    assert_status(_checkout(base_url, s, "b1", 999999999), 404)


def test_04_remaining_subtracts_pending(base_url, s):
    assert _public_item(base_url, s, s.a)["remaining"] == 5
    s.orders["b1"] = _ok_order(base_url, s, "b1", s.a, 2, depositor="128구구")
    a = _public_item(base_url, s, s.a)
    assert a["remaining"] == 3 and a["isSoldOut"] is False and a["isPurchasable"] is True
    s.orders["b2"] = _ok_order(base_url, s, "b2", s.a, 3, depositor="김구구")
    a = _public_item(base_url, s, s.a)
    assert a["remaining"] == 0 and a["isSoldOut"] is True and a["isPurchasable"] is False
    sold_out = get_data(_checkout(base_url, s, "b3", s.a))
    assert sold_out["ticket"]["remaining"] == 0 and sold_out["ticket"]["isPurchasable"] is False
    # 살 수 없으면 계좌를 주지 않는다 (입금 방지)
    assert sold_out["account"] is None
    # 호스트 T-1·D-1: 재고(remaining)는 그대로, 승인 대기는 별도 필드 (결정 2026-10-05)
    manage = next(t for t in get_data(requests.get(_ev(base_url, s, "/ticket-items/manage"), headers=_h(s, "guest"))) if t["ticketItemId"] == s.a)
    assert manage["remaining"] == 5 and manage["pendingApproveCount"] == 5 and manage["hasPendingOrders"] is True
    dash = get_data(requests.get(_ev(base_url, s, "/dashboard"), headers=_h(s, "guest")))
    assert next(t for t in dash["tickets"]["items"] if t["ticketItemId"] == s.a)["pendingApproveCount"] == 5
    # 화면과 주문 재고 검사(#723)가 같은 기준
    resp = _v2_order(base_url, s, "late", s.a, 1)
    assert_status(resp, 400)
    assert resp.json()["code"] == "Ticket_Item_400_1", resp.text
    # 다른 티켓은 영향 없음
    assert _public_item(base_url, s, s.b)["remaining"] == 30
    # 거절하면 대기에서 빠져 잔여가 돌아온다
    refuse = requests.post(_ev(base_url, s, f"/orders/{s.orders['b2']}/refuse"), json={"reasonType": "SOLD_OUT"}, headers=_h(s, "manager"))
    assert_status(refuse, 200)
    assert _public_item(base_url, s, s.a)["remaining"] == 3


def test_05_depositor_search_and_excel(base_url, s):
    s.orders["evil"] = _ok_order(base_url, s, "evil", s.b, 1, depositor="=HYPERLINK(\"x\")")
    found = _orders(base_url, s, searchType="DEPOSITOR_NAME", keyword="구구")
    assert {o["orderUuid"] for o in found["orders"]["content"]} == {s.orders["b1"], s.orders["b2"]}
    assert found["counts"]["all"] == 2 and found["counts"]["refused"] == 1
    assert {o["depositorName"] for o in found["orders"]["content"]} == {"128구구", "김구구"}
    # 이름 검색은 회원 이름 기준 그대로
    assert _orders(base_url, s, keyword="구구")["counts"]["all"] == 0

    resp = requests.get(_ev(base_url, s, "/orders/export"), headers=_h(s, "guest"))
    assert_status(resp, 200)
    rows = _xlsx_rows(resp.content)
    assert rows[0] == ["주문번호", "주문자", "연락처", "입금자명", "티켓", "매수", "결제금액", "주문일시", "상태", "환불", "거절·취소 사유"]
    depositors = {r[3] for r in rows[1:]}
    assert {"128구구", "김구구", "'=HYPERLINK(\"x\")"} <= depositors, depositors
    filtered = _xlsx_rows(requests.get(_ev(base_url, s, "/orders/export"), params={"searchType": "DEPOSITOR_NAME", "keyword": "128"}, headers=_h(s, "guest")).content)
    assert [r[3] for r in filtered[1:]] == ["128구구"]


def test_06_purchase_limit_concurrent_pending(base_url, s):
    """같은 사용자가 승인 대기 2장 + 3장(합 5 > 1인 4장)을 동시에 → 같은 티켓 락 안의 승인 대기 합산 검사로 1건만 접수"""
    for who in ["lim1", "lim2", "lim3"]:
        with ThreadPoolExecutor(max_workers=2) as pool:
            futures = [pool.submit(_v2_order, base_url, s, who, s.b, q) for q in (2, 3)]
            results = [f.result() for f in futures]
        ok = [r for r in results if r.status_code == 200]
        rejected = [r for r in results if r.status_code != 200]
        assert len(ok) == 1, [r.text for r in results]
        assert [r.json()["code"] for r in rejected] == ["Order_400_15"], [r.text for r in results]
        resp = requests.get(f"{base_url}/v2/me/orders", headers=_h(s, who))
        pending = sum(o["quantity"] for o in get_data(resp)["content"])
        assert pending in (2, 3)
        # 순차로도 제한을 넘지 못한다 (대기 + 이번 = 5 > 4), 딱 4 까지는 된다
        resp = _v2_order(base_url, s, who, s.b, 5 - pending)
        assert_status(resp, 400)
        assert resp.json()["code"] == "Order_400_15"
        assert_status(_v2_order(base_url, s, who, s.b, 4 - pending, depositor="경계"), 200)


def _approve_and_cancel(base_url, s, who, reason="취소"):
    # 입금자명을 매번 다르게 해 10초 중복 요청 판정(같은 사용자·티켓·수량·입금자명)에 걸리지 않게 한다
    o = _ok_order(base_url, s, who, s.b, 1, depositor=f"기준{uuid.uuid4().hex[:8]}")
    assert_status(requests.post(_ev(base_url, s, f"/orders/{o}/approve"), headers=_h(s, "manager")), 200)
    assert_status(requests.post(_ev(base_url, s, f"/orders/{o}/cancel"), json={"reason": reason}, headers=_h(s, "manager")), 200)
    return o


def _later_reference(base_url, s, who):
    """'알림 없음' 검증의 기준점: 같은 사용자에게 나중에 일어난 알림(새 주문 승인 → 호스트 취소)이 도착할 때까지 기다린다 (고정 대기 대신)"""
    ref = _approve_and_cancel(base_url, s, who, "기준")
    assert _wait_notification(base_url, s, who, "ORDER_CANCELED_BY_HOST", ref), "기준 알림이 도착하지 않음"


def test_07_host_cancel_and_refund_complete_notifications(base_url, s):
    # v2: 승인 → 호스트 취소 → 환불 완료
    o1 = _approve_and_cancel(base_url, s, "cancel1", "공연 취소")
    canceled = _wait_notification(base_url, s, "cancel1", "ORDER_CANCELED_BY_HOST", o1)
    assert len(canceled) == 1 and canceled[0]["body"].endswith("사유: 공연 취소. 주문상세에서 환불 계좌를 입력해 주세요."), canceled
    assert_status(requests.post(_ev(base_url, s, f"/refunds/{o1}/complete"), headers=_h(s, "manager")), 200)
    assert len(_wait_notification(base_url, s, "cancel1", "ORDER_REFUND_COMPLETED", o1)) == 1
    # 다시 눌러도 1건 (멱등)
    assert_status(requests.post(_ev(base_url, s, f"/refunds/{o1}/complete"), headers=_h(s, "manager")), 200)
    _later_reference(base_url, s, "cancel1")
    assert len(_notifications(base_url, s, "cancel1", "ORDER_REFUND_COMPLETED")) == 1
    assert not _notifications(base_url, s, "cancel1", "ORDER_REFUSED")

    # v1: 승인 → v1 취소 → v1 환불 완료 2번 (uk 로 1건)
    o2 = _ok_order(base_url, s, "cancel2", s.b, 1)
    assert_status(requests.post(f"{base_url}/v1/events/{s.event_id}/orders/{o2}/approve", headers=_h(s, "master")), 200)
    assert_status(requests.post(f"{base_url}/v1/events/{s.event_id}/orders/{o2}/cancel", json={"reason": "v1 취소"}, headers=_h(s, "master")), 200)
    assert len(_wait_notification(base_url, s, "cancel2", "ORDER_CANCELED_BY_HOST", o2)) == 1
    for _ in range(2):
        assert_status(requests.patch(f"{base_url}/v1/events/{s.event_id}/refunds/{o2}/complete", headers=_h(s, "master")), 200)
    assert _wait_notification(base_url, s, "cancel2", "ORDER_REFUND_COMPLETED", o2)
    _later_reference(base_url, s, "cancel2")
    assert len([n for n in _notifications(base_url, s, "cancel2", "ORDER_REFUND_COMPLETED") if n["target"]["id"] == o2]) == 1

    # 거절은 거절 알림만, 0원(무료) 환불 완료는 알림 없음
    assert _wait_notification(base_url, s, "b2", "ORDER_REFUSED", s.orders["b2"])
    f = _ok_order(base_url, s, "free1", s.free, 1, depositor=None, method="FREE")
    assert_status(requests.post(_ev(base_url, s, f"/orders/{f}/refuse"), json={"reasonType": "SOLD_OUT"}, headers=_h(s, "manager")), 200)
    assert _wait_notification(base_url, s, "free1", "ORDER_REFUSED", f)
    assert_status(requests.post(_ev(base_url, s, f"/refunds/{f}/complete"), headers=_h(s, "manager")), 200)
    _later_reference(base_url, s, "free1")
    assert not _notifications(base_url, s, "free1", "ORDER_REFUND_COMPLETED")
    assert not [n for n in _notifications(base_url, s, "free1", "ORDER_CANCELED_BY_HOST") if n["target"]["id"] == f]


def test_09_fcfs_host_cancel_and_wrong_refund_complete(base_url, s):
    """결정 2026-10-05: 무료 선착순(결제형)도 호스트 취소 알림 (v2·v1). 승인 완료 주문을 v1 에서 잘못 환불 완료해도 알림 없음"""
    for who, path in [("fcfs_v2", "v2"), ("fcfs_v1", "v1")]:
        o = _ok_order(base_url, s, who, s.fcfs, 1, depositor=None, method="FREE")
        url = _ev(base_url, s, f"/orders/{o}/cancel") if path == "v2" else f"{base_url}/v1/events/{s.event_id}/orders/{o}/cancel"
        assert_status(requests.post(url, json={"reason": f"{path} 무료 취소"}, headers=_h(s, "master")), 200)
        found = _wait_notification(base_url, s, who, "ORDER_CANCELED_BY_HOST", o)
        assert len(found) == 1 and found[0]["body"].endswith(f"사유: {path} 무료 취소"), found
    # 사용자 본인 취소는 호스트 취소 알림 대상 아님
    o = _ok_order(base_url, s, "fcfs_self", s.fcfs, 1, depositor=None, method="FREE")
    assert_status(requests.post(f"{base_url}/v2/me/orders/{o}/cancel", json={}, headers=_h(s, "fcfs_self")), 200)
    _later_reference(base_url, s, "fcfs_self")
    assert not [n for n in _notifications(base_url, s, "fcfs_self", "ORDER_CANCELED_BY_HOST") if n["target"]["id"] == o]

    # 승인 완료 주문 v1 환불 완료 → 알림 없음
    approved = _ok_order(base_url, s, "wrong", s.b, 1)
    assert_status(requests.post(_ev(base_url, s, f"/orders/{approved}/approve"), headers=_h(s, "manager")), 200)
    assert_status(requests.patch(f"{base_url}/v1/events/{s.event_id}/refunds/{approved}/complete", headers=_h(s, "master")), 200)
    _later_reference(base_url, s, "wrong")
    assert not _notifications(base_url, s, "wrong", "ORDER_REFUND_COMPLETED")

def _make_admin(e2e_db, email):
    """운영 어드민 API 는 DB 의 account_role 을 매 요청 읽는다 (JwtTokenFilter)"""
    e2e_db.query(f"UPDATE tbl_user SET account_role='ADMIN' WHERE email='{email}'")


def test_08_admin_paths_notifications(base_url, s, e2e_db):
    """운영 어드민(DuDoong-Admin 모듈, Api 서버의 /internal-api) 취소·환불 확인·환불 상태 변경도 같은 도메인 이벤트 → 같은 알림"""
    _make_admin(e2e_db, _email("admin"))
    internal = base_url.replace("/api", "/internal-api")
    o1 = _ok_order(base_url, s, "adm1", s.b, 1)
    assert_status(requests.post(_ev(base_url, s, f"/orders/{o1}/approve"), headers=_h(s, "manager")), 200)
    assert_status(requests.post(f"{internal}/v1/orders/{o1}/cancel", json={"reason": "운영 취소"}, headers=_h(s, "admin")), 200)
    found = _wait_notification(base_url, s, "adm1", "ORDER_CANCELED_BY_HOST", o1)
    assert len(found) == 1 and found[0]["body"].endswith("사유: 운영 취소. 주문상세에서 환불 계좌를 입력해 주세요."), found
    assert_status(requests.patch(f"{internal}/v1/refunds/{o1}/complete", headers=_h(s, "admin")), 200)
    assert len(_wait_notification(base_url, s, "adm1", "ORDER_REFUND_COMPLETED", o1)) == 1

    o2 = _ok_order(base_url, s, "adm2", s.b, 1)
    assert_status(requests.post(_ev(base_url, s, f"/orders/{o2}/refuse"), json={"reasonType": "DEPOSIT_UNCONFIRMED"}, headers=_h(s, "manager")), 200)
    assert_status(requests.patch(f"{internal}/v1/orders/{o2}/refund-status", json={"refundStatus": "REFUND_COMPLETED"}, headers=_h(s, "admin")), 200)
    assert len(_wait_notification(base_url, s, "adm2", "ORDER_REFUND_COMPLETED", o2)) == 1
    assert not _notifications(base_url, s, "adm2", "ORDER_CANCELED_BY_HOST")
