"""
v2 환불 계좌 입력·수정 O-5 E2E (#728, 사용자 결정 2026-10-05: 호스트 거절·취소 주문의 환불 계좌를 사용자가 입력).

시나리오: 두둥티켓(계좌이체·승인형) 주문 → 호스트 거절 → 주문상세(O-3) 입력 필요 → 계좌 입력 → 주문자에게는 뒤 4자리만 →
호스트 R-2·F-1 은 매니저 이상만 전체 계좌(+ 마지막 입력·수정 시각) → 계좌 변경 시 호스트 마스터·매니저 알림 → 환불 완료 → 수정 거부(Order_400_28).
호스트 승인 후 취소·사용자 취소(O-4) 주문의 입력·수정, 입금 미확인 거절(입력 필요 아님·안내 없음), v1 주문(대상 아님), 남의 주문, 알림 딥링크(주문상세) 확인.
MySQL 경합: 주문 행을 별도 세션으로 잡아 둔 채 v1 환불 완료(주문 락 없음) → 계좌 입력 순으로 보내고, 입력이 완료 뒤에 판정되는지 확인.

DB 직접 접근(v1 주문 흉내, 행 잠금 세션)은 conftest 의 e2e_db fixture(#737), 잠금 대기 조회는 e2e_db_root(못 읽으면 경합 테스트 skip).
"""
import re
import subprocess
import threading
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
SECTIONS = [{"title": "공연 소개", "content": "<p>환불 계좌 테스트</p>", "sortOrder": 0}]
ACCOUNT = {"bankName": "신한은행", "accountHolder": "고스락", "accountNumber": "110-123-456789"}
REFUND_ACCOUNT = {"bankName": "국민은행", "accountHolder": "홍길동", "accountNumber": "123-45-678901"}
GUIDE = "주문상세에서 환불 계좌를 입력해 주세요."
PEOPLE = ["master", "manager", "guest", "refused", "unconfirmed", "v1", "canceled", "self", "other", "race1", "race2"]


class State:
    tokens: dict = {}
    emails: dict = {}
    event_id: int = 0
    ticket_id: int = 0
    orders: dict = {}


@pytest.fixture(scope="module")
def s():
    return State()


# conftest e2e_db fixture 를 이 모듈의 헬퍼(_sql, _RowLock 등)가 쓰도록 묶는다 (#737)
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


def _ev(base_url, s, path=""):
    return f"{base_url}/v2/events/{s.event_id}{path}"


def _order(base_url, s, who):
    resp = requests.post(f"{base_url}/v2/me/orders", json={
        "eventId": s.event_id, "ticketItemId": s.ticket_id, "quantity": 1,
        "options": {"applyToAll": True, "answers": []}, "perTicketOptions": None,
        "paymentChannel": "BANK_TRANSFER", "depositorName": "입금자", "agreeRefundPolicy": True,
    }, headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)["orderUuid"]


def _detail(base_url, s, who, order_uuid):
    resp = requests.get(f"{base_url}/v2/me/orders/{order_uuid}", headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)


def _put(base_url, s, who, order_uuid, body=None):
    return requests.put(f"{base_url}/v2/me/orders/{order_uuid}/refund-account", json=body or REFUND_ACCOUNT, headers=_h(s, who))


def _host_detail(base_url, s, who, order_uuid):
    resp = requests.get(_ev(base_url, s, f"/orders/{order_uuid}"), headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)


def _notifications(base_url, s, who, type_, order_uuid, count=1):
    """who 의 type_ 알림 중 order_uuid 대상인 것. count 건이 될 때까지 최대 10초 기다린다"""
    deadline = time.time() + 10
    while True:
        resp = requests.get(f"{base_url}/v2/me/notifications", params={"size": 50}, headers=_h(s, who))
        assert_status(resp, 200)
        found = [n for n in get_data(resp)["content"] if n["type"] == type_ and n["target"]["targetId"] == order_uuid]
        if len(found) >= count or time.time() > deadline:
            return found
        time.sleep(0.2)


def test_01_setup(base_url, s):
    for who in PEOPLE:
        email = f"refund728-{who}-{RUN}@dudoong.com"
        resp = requests.post(f"{base_url}/v1/auth/oauth/local/login", json={
            "email": email, "name": f"환불{who}", "phoneNumber": "010-0000-0000", "profileImage": None, "marketingAgree": False,
        })
        assert_status(resp, 200)
        s.tokens[who], s.emails[who] = get_data(resp)["accessToken"], email
    resp = requests.post(f"{base_url}/v2/hosts", json={"name": f"환불{RUN[:5]}", "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    host_id = get_data(resp)["hostId"]
    assert_status(requests.post(f"{base_url}/v2/hosts/{host_id}/members", json={"members": [
        {"email": s.emails["manager"], "role": "MANAGER"}, {"email": s.emails["guest"], "role": "GUEST"}]}, headers=_h(s, "master")), 200)
    resp = requests.post(f"{base_url}/v2/events", json={
        "hostId": host_id, "name": "환불계좌공연", "startAt": START.strftime(FMT), "endAt": END.strftime(FMT), "hasTicket": True}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    s.event_id = get_data(resp)["eventId"]
    resp = requests.post(_ev(base_url, s, "/ticket-items"), json={
        "payType": "DUDOONG", "name": "일반", "description": "일반", "price": 6000, "supplyCount": 50, "account": ACCOUNT,
        "approvalRequired": True, "isQuantityPublic": True, "purchaseLimit": 4, "saleStartAt": None, "saleEndAt": None,
    }, headers=_h(s, "manager"))
    assert_status(resp, 200)
    s.ticket_id = get_data(resp)["ticketItemId"]
    key = get_data(requests.post(_ev(base_url, s, "/images"), json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "manager")))["key"]
    assert_status(requests.patch(_ev(base_url, s, "/basic"), json={"posterImageKey": key, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]}, headers=_h(s, "manager")), 200)
    assert_status(requests.put(_ev(base_url, s, "/sections"), json={"sections": SECTIONS}, headers=_h(s, "manager")), 200)
    assert_status(requests.post(_ev(base_url, s, "/open"), headers=_h(s, "manager")), 200)


def test_02_refused_input_host_view_complete_then_locked(base_url, s):
    order_uuid = _order(base_url, s, "refused")
    assert _code(_put(base_url, s, "refused", order_uuid)) == "Order_400_27", "승인 대기(환불 요청 없음)는 대상 아님"
    assert_status(requests.post(_ev(base_url, s, f"/orders/{order_uuid}/refuse"), json={"reasonType": "AMOUNT_MISMATCH"}, headers=_h(s, "manager")), 200)

    d = _detail(base_url, s, "refused", order_uuid)
    assert d["refundAccountRequired"] is True and d["canEditRefundAccount"] is True and d["refundAccount"] is None

    # 거절 알림은 주문상세로 이동 (target = ORDER / orderUuid) + 계좌 입력 안내
    found = _notifications(base_url, s, "refused", "ORDER_REFUSED", order_uuid)
    assert found and found[0]["target"]["type"] == "ORDER"
    assert found[0]["body"].endswith(GUIDE), found[0]["body"]

    assert _code(_put(base_url, s, "other", order_uuid)) == "Order_404_1"
    resp = _put(base_url, s, "refused", order_uuid, {**REFUND_ACCOUNT, "accountNumber": "12ab"})
    assert resp.status_code == 400

    resp = _put(base_url, s, "refused", order_uuid)
    assert_status(resp, 200)
    d = get_data(resp)
    assert d["refundAccountRequired"] is False and d["canEditRefundAccount"] is True
    assert d["refundAccount"] == {"bankName": "국민은행", "accountHolder": "홍길동", "maskedAccountNumber": "*********8901"}

    # 호스트: 매니저 이상만 전체 계좌 + 마지막 입력·수정 시각, 일반 멤버는 null (R-2·F-1)
    host_account = _host_detail(base_url, s, "manager", order_uuid)["refundAccount"]
    assert host_account["accountNumber"] == "123-45-678901" and host_account["updatedAt"], host_account
    # 날짜 형식은 다른 필드와 같다 (#755)
    assert re.fullmatch(r"\d{4}\.\d{2}\.\d{2} \d{2}:\d{2}", host_account["updatedAt"]), host_account
    assert _host_detail(base_url, s, "guest", order_uuid)["refundAccount"] is None
    rows = get_data(requests.get(_ev(base_url, s, "/refunds"), headers=_h(s, "manager")))["content"]
    row_account = next(r for r in rows if r["orderUuid"] == order_uuid)["refundAccount"]
    assert row_account["bankName"] == "국민은행" and re.fullmatch(r"\d{4}\.\d{2}\.\d{2} \d{2}:\d{2}", row_account["updatedAt"]), row_account

    # 같은 값 재입력은 변경 아님. 다른 값으로 수정 → 호스트 마스터·매니저 변경 알림 1건씩 (첫 입력은 알림 없음)
    assert_status(_put(base_url, s, "refused", order_uuid), 200)
    assert_status(_put(base_url, s, "refused", order_uuid, {"bankName": "우리은행", "accountHolder": "김철수", "accountNumber": "1002 123 456789"}), 200)
    assert _host_detail(base_url, s, "manager", order_uuid)["refundAccount"]["accountNumber"] == "1002123456789"
    for who in ("master", "manager"):
        changed = _notifications(base_url, s, who, "REFUND_ACCOUNT_CHANGED", order_uuid)
        assert len(changed) == 1 and changed[0]["target"]["type"] == "ORDER", changed
    assert _notifications(base_url, s, "guest", "REFUND_ACCOUNT_CHANGED", order_uuid, count=0) == []
    assert_status(requests.post(_ev(base_url, s, f"/refunds/{order_uuid}/complete"), headers=_h(s, "manager")), 200)
    resp = _put(base_url, s, "refused", order_uuid)
    assert resp.status_code == 400 and _code(resp) == "Order_400_28"
    d = _detail(base_url, s, "refused", order_uuid)
    assert d["canEditRefundAccount"] is False and d["refundAccount"]["bankName"] == "우리은행"


def test_03_deposit_unconfirmed_and_v1_order(base_url, s):
    # 입금 미확인 거절: 입력 필요 아님 + 알림 안내 없음, 입력은 가능
    unconfirmed = _order(base_url, s, "unconfirmed")
    assert_status(requests.post(_ev(base_url, s, f"/orders/{unconfirmed}/refuse"), json={"reasonType": "DEPOSIT_UNCONFIRMED"}, headers=_h(s, "manager")), 200)
    d = _detail(base_url, s, "unconfirmed", unconfirmed)
    assert d["refundAccountRequired"] is False and d["canEditRefundAccount"] is True
    found = _notifications(base_url, s, "unconfirmed", "ORDER_REFUSED", unconfirmed)
    assert found and GUIDE not in found[0]["body"], found
    assert_status(_put(base_url, s, "unconfirmed", unconfirmed), 200)

    # v1 주문(결제 채널 없음 — v1 앱 주문을 DB 로 흉내): 거절돼도 대상 아님, 안내 없음
    v1 = _order(base_url, s, "v1")
    _sql(f"UPDATE tbl_order SET payment_channel = NULL WHERE uuid = '{v1}'")
    assert_status(requests.post(_ev(base_url, s, f"/orders/{v1}/refuse"), json={"reasonType": "AMOUNT_MISMATCH"}, headers=_h(s, "manager")), 200)
    assert _code(_put(base_url, s, "v1", v1)) == "Order_400_27"
    d = _detail(base_url, s, "v1", v1)
    assert d["refundAccountRequired"] is False and d["canEditRefundAccount"] is False
    found = _notifications(base_url, s, "v1", "ORDER_REFUSED", v1)
    assert found and GUIDE not in found[0]["body"], found


def test_04_host_cancel_and_user_cancel(base_url, s):
    canceled = _order(base_url, s, "canceled")
    assert_status(requests.post(_ev(base_url, s, f"/orders/{canceled}/approve"), headers=_h(s, "manager")), 200)
    assert _code(_put(base_url, s, "canceled", canceled)) == "Order_400_27", "승인 완료(환불 요청 없음)는 대상 아님"
    assert_status(requests.post(_ev(base_url, s, f"/orders/{canceled}/cancel"), json={"reason": "공연 취소"}, headers=_h(s, "manager")), 200)
    assert _detail(base_url, s, "canceled", canceled)["refundAccountRequired"] is True
    assert_status(_put(base_url, s, "canceled", canceled), 200)

    own = _order(base_url, s, "self")
    assert_status(requests.post(f"{base_url}/v2/me/orders/{own}/cancel", json={"refundAccount": REFUND_ACCOUNT}, headers=_h(s, "self")), 200)
    d = _detail(base_url, s, "self", own)
    assert d["refundAccountRequired"] is False and d["canEditRefundAccount"] is True
    assert_status(_put(base_url, s, "self", own, {"bankName": "하나은행", "accountHolder": "본인", "accountNumber": "111-222-333"}), 200)
    assert _detail(base_url, s, "self", own)["refundAccount"]["bankName"] == "하나은행"


# ===== MySQL 경합 (결정적) =====

# performance_schema 를 root 로 못 읽으면 결정적 경합 테스트는 skip (conftest e2e_db_root, E2E_DB_ROOT_PASSWORD)
needs_lock_inspection = pytest.mark.usefixtures("e2e_db_root")


def _lock_waits():
    return int(DB.query(
        "SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks l "
        f"ON l.ENGINE_LOCK_ID = w.REQUESTING_ENGINE_LOCK_ID WHERE l.OBJECT_SCHEMA = '{DB.name}'",
        root=True, database=False,
    ))


class _RowLock:
    """별도 세션에서 BEGIN; <잠금 SELECT>; 를 실행해 둔 채로 있다가 release() 에서 COMMIT.
    홀더가 다른 잠금에 막혀도 무한 대기하지 않도록 세션 잠금 대기를 10초로 둔다 (넘으면 mysql 이 오류로 끝나 아래 readline 이 실패)"""

    def __init__(self, lock_sql):
        self.p = subprocess.Popen(
            DB.command("--unbuffered", "-N"), env=DB.env(),
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
        )
        self.p.stdin.write(f"SET SESSION innodb_lock_wait_timeout = 10;\nBEGIN;\n{lock_sql};\nSELECT 'LOCKED';\n")
        self.p.stdin.flush()
        while True:
            line = self.p.stdout.readline()
            assert line, self.p.stderr.read()
            if line.strip() == "LOCKED":
                break

    def release(self):
        self.p.stdin.write("COMMIT;\n")
        self.p.stdin.close()
        self.p.wait(timeout=10)


def _await_waits(n, timeout=6.0):
    deadline = time.time() + timeout
    while time.time() < deadline:
        if _lock_waits() >= n:
            return
        time.sleep(0.05)
    raise AssertionError(f"잠금 대기 {n}건이 안 됨 (현재 {_lock_waits()})")


@needs_lock_inspection
def test_05_race_v1_complete_then_put(base_url, s):
    """주문 행을 별도 세션으로 잡은 채 v1 환불 완료(주문 락 없음, 행 UPDATE 대기) → 계좌 입력(행 잠금 대기) 순으로 보내고 해제:
    입력은 완료 뒤에 판정되어 Order_400_28, 계좌가 저장되지 않는다.
    입력이 행 잠금 없이 읽으면 대기 2건이 되지 않아 여기서 실패한다 (대기 확인 시간 초과는 삼키지 않는다)"""
    for who in ("race1", "race2"):
        order_uuid = _order(base_url, s, who)
        assert_status(requests.post(_ev(base_url, s, f"/orders/{order_uuid}/refuse"), json={"reasonType": "AMOUNT_MISMATCH"}, headers=_h(s, "manager")), 200)
        order_id = _sql(f"SELECT order_id FROM tbl_order WHERE uuid = '{order_uuid}'")
        results, errors = {}, {}

        def run(key, call):
            try:
                results[key] = call()
            except Exception as e:  # 요청 스레드의 예외를 그대로 다시 던진다 (KeyError 로 가려지지 않게)
                errors[key] = e

        holder = _RowLock(f"SELECT order_id FROM tbl_order WHERE order_id = {order_id} FOR UPDATE")
        try:
            t1 = threading.Thread(target=run, args=("complete", lambda: requests.patch(
                f"{base_url}/v1/events/{s.event_id}/refunds/{order_uuid}/complete", headers=_h(s, "master"))))
            t1.start()
            _await_waits(1)
            t2 = threading.Thread(target=run, args=("put", lambda: _put(base_url, s, who, order_uuid)))
            t2.start()
            _await_waits(2)
        finally:
            holder.release()
        t1.join(30)
        t2.join(30)
        for key in ("complete", "put"):
            if key in errors:
                raise errors[key]
            assert key in results, f"{key} 요청이 30초 안에 끝나지 않음"
        assert results["complete"].status_code == 200, results["complete"].text
        assert results["put"].status_code == 400 and _code(results["put"]) == "Order_400_28", results["put"].text
        assert _sql(f"SELECT COUNT(*) FROM tbl_order_refund_account WHERE order_id = {order_id}") == "0"
