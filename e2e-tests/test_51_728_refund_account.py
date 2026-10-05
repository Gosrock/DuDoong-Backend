"""
v2 환불 계좌 입력·수정 O-5 E2E (#728, 사용자 결정 2026-10-05: 호스트 거절·취소 주문의 환불 계좌를 사용자가 입력).

시나리오: 두둥티켓(계좌이체·승인형) 주문 → 호스트 거절 → 주문상세(O-3) 입력 필요 → 계좌 입력 → 주문자에게는 뒤 4자리만 →
호스트 R-2·F-1 은 매니저 이상만 전체 계좌 → 환불 완료 → 수정 거부(Order_400_28).
호스트 승인 후 취소·사용자 취소(O-4) 주문의 입력·수정, 대상 아님·남의 주문, 알림 딥링크(주문상세) 확인.
MySQL 경합: 주문 행을 별도 세션으로 잡아 둔 채 v1 환불 완료(주문 락 없음) → 계좌 입력 순으로 보내고, 입력이 완료 뒤에 판정되는지 확인.

DB 직접 접근은 E2E_DB(기본 dudoong) 의 로컬 MySQL, performance_schema 조회는 E2E_DB_ROOT_PASSWORD(기본 docker-compose 로컬값) — 못 읽으면 경합 테스트 skip.
"""
import os
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
ACCOUNT = {"bank": "신한은행", "holder": "고스락", "number": "110-123-456789"}
REFUND_ACCOUNT = {"bankName": "국민은행", "accountHolder": "홍길동", "accountNumber": "123-45-678901"}
DB_NAME = os.environ.get("E2E_DB", "dudoong")
# 기본값은 docker-compose.yml 의 로컬 개발용 root 비밀번호 (운영 값 아님)
ROOT_PW = os.environ.get("E2E_DB_ROOT_PASSWORD", "dudoong")
PEOPLE = ["master", "manager", "guest", "refused", "canceled", "self", "other", "race1", "race2"]


class State:
    tokens: dict = {}
    emails: dict = {}
    event_id: int = 0
    ticket_id: int = 0
    orders: dict = {}


@pytest.fixture(scope="module")
def s():
    return State()


def _h(s, who):
    return {"Authorization": f"Bearer {s.tokens[who]}"}


def _code(resp):
    return resp.json().get("code")


def _ev(base_url, s, path=""):
    return f"{base_url}/v2/events/{s.event_id}{path}"


def _order(base_url, s, who):
    resp = requests.post(f"{base_url}/v2/orders", json={
        "eventId": s.event_id, "ticketItemId": s.ticket_id, "quantity": 1,
        "options": {"applyToAll": True, "answers": []}, "perTicketOptions": None,
        "paymentMethod": "BANK_TRANSFER", "depositorName": "입금자", "agreeRefundPolicy": True,
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


def _sql(sql):
    result = subprocess.run(
        ["mysql", "-h", "127.0.0.1", "-P", "13306", "-u", "dudoong", "-pdudoong", DB_NAME, "-N", "-e", sql],
        capture_output=True, text=True,
    )
    assert result.returncode == 0, result.stderr
    return result.stdout.strip()


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
    assert_status(requests.put(_ev(base_url, s, "/sections"), json=SECTIONS, headers=_h(s, "manager")), 200)
    assert_status(requests.post(_ev(base_url, s, "/open"), headers=_h(s, "manager")), 200)


def test_02_refused_input_host_view_complete_then_locked(base_url, s):
    order_uuid = _order(base_url, s, "refused")
    assert _code(_put(base_url, s, "refused", order_uuid)) == "Order_400_27", "승인 대기(환불 요청 없음)는 대상 아님"
    assert_status(requests.post(_ev(base_url, s, f"/orders/{order_uuid}/refuse"), json={"reasonType": "DEPOSIT_UNCONFIRMED"}, headers=_h(s, "manager")), 200)

    d = _detail(base_url, s, "refused", order_uuid)
    assert d["refundAccountRequired"] is True and d["refundAccountEditable"] is True and d["refundAccount"] is None

    # 거절 알림은 주문상세로 이동 (target = ORDER / orderUuid)
    deadline = time.time() + 10
    found = []
    while time.time() < deadline and not found:
        resp = requests.get(f"{base_url}/v2/me/notifications", params={"size": 50}, headers=_h(s, "refused"))
        found = [n for n in get_data(resp)["content"] if n["type"] == "ORDER_REFUSED" and n["target"]["id"] == order_uuid]
        time.sleep(0.2)
    assert found and found[0]["target"]["type"] == "ORDER"

    assert _code(_put(base_url, s, "other", order_uuid)) == "Order_404_1"
    resp = _put(base_url, s, "refused", order_uuid, {**REFUND_ACCOUNT, "accountNumber": "12ab"})
    assert resp.status_code == 400

    resp = _put(base_url, s, "refused", order_uuid)
    assert_status(resp, 200)
    d = get_data(resp)
    assert d["refundAccountRequired"] is False and d["refundAccountEditable"] is True
    assert d["refundAccount"] == {"bankName": "국민은행", "accountHolder": "홍길동", "maskedAccountNumber": "*********8901"}

    # 호스트: 매니저 이상만 전체 계좌, 일반 멤버는 null (R-2·F-1)
    assert _host_detail(base_url, s, "manager", order_uuid)["refundAccount"]["accountNumber"] == "123-45-678901"
    assert _host_detail(base_url, s, "guest", order_uuid)["refundAccount"] is None
    rows = get_data(requests.get(_ev(base_url, s, "/refunds"), headers=_h(s, "manager")))["content"]
    assert next(r for r in rows if r["orderUuid"] == order_uuid)["refundAccount"]["bankName"] == "국민은행"

    # 수정 → 환불 완료 → 수정 거부
    assert_status(_put(base_url, s, "refused", order_uuid, {"bankName": "우리은행", "accountHolder": "김철수", "accountNumber": "1002 123 456789"}), 200)
    assert _host_detail(base_url, s, "manager", order_uuid)["refundAccount"]["accountNumber"] == "1002123456789"
    assert_status(requests.post(_ev(base_url, s, f"/refunds/{order_uuid}/complete"), headers=_h(s, "manager")), 200)
    resp = _put(base_url, s, "refused", order_uuid)
    assert resp.status_code == 400 and _code(resp) == "Order_400_28"
    d = _detail(base_url, s, "refused", order_uuid)
    assert d["refundAccountEditable"] is False and d["refundAccount"]["bankName"] == "우리은행"


def test_03_host_cancel_and_user_cancel(base_url, s):
    canceled = _order(base_url, s, "canceled")
    assert_status(requests.post(_ev(base_url, s, f"/orders/{canceled}/approve"), headers=_h(s, "manager")), 200)
    assert _code(_put(base_url, s, "canceled", canceled)) == "Order_400_27", "승인 완료(환불 요청 없음)는 대상 아님"
    assert_status(requests.post(_ev(base_url, s, f"/orders/{canceled}/cancel"), json={"reason": "공연 취소"}, headers=_h(s, "manager")), 200)
    assert _detail(base_url, s, "canceled", canceled)["refundAccountRequired"] is True
    assert_status(_put(base_url, s, "canceled", canceled), 200)

    own = _order(base_url, s, "self")
    assert_status(requests.post(f"{base_url}/v2/me/orders/{own}/cancel", json={"refundAccount": REFUND_ACCOUNT}, headers=_h(s, "self")), 200)
    d = _detail(base_url, s, "self", own)
    assert d["refundAccountRequired"] is False and d["refundAccountEditable"] is True
    assert_status(_put(base_url, s, "self", own, {"bankName": "하나은행", "accountHolder": "본인", "accountNumber": "111-222-333"}), 200)
    assert _detail(base_url, s, "self", own)["refundAccount"]["bankName"] == "하나은행"


# ===== MySQL 경합 (결정적) =====

def _perf_schema_available():
    result = subprocess.run(
        ["mysql", "-h", "127.0.0.1", "-P", "13306", "-uroot", f"-p{ROOT_PW}", "-N", "-e", "SELECT COUNT(*) FROM performance_schema.data_lock_waits"],
        capture_output=True, text=True,
    )
    return result.returncode == 0


def _lock_waits():
    result = subprocess.run(
        ["mysql", "-h", "127.0.0.1", "-P", "13306", "-uroot", f"-p{ROOT_PW}", "-N", "-e",
         "SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks l "
         f"ON l.ENGINE_LOCK_ID = w.REQUESTING_ENGINE_LOCK_ID WHERE l.OBJECT_SCHEMA = '{DB_NAME}'"],
        capture_output=True, text=True,
    )
    assert result.returncode == 0, result.stderr
    return int(result.stdout.strip())


def _await_waits(n, timeout=6.0):
    deadline = time.time() + timeout
    while time.time() < deadline:
        if _lock_waits() >= n:
            return
        time.sleep(0.05)
    raise AssertionError(f"잠금 대기 {n}건이 안 됨 (현재 {_lock_waits()})")


@pytest.mark.skipif(not _perf_schema_available(), reason="performance_schema 를 읽을 수 없음 (E2E_DB_ROOT_PASSWORD 확인)")
def test_04_race_v1_complete_then_put(base_url, s):
    """주문 행을 별도 세션으로 잡은 채 v1 환불 완료(주문 락 없음, 행 UPDATE 대기) → 계좌 입력(행 잠금 대기) 순으로 보내고 해제:
    입력은 완료 뒤에 판정되어 Order_400_28, 계좌가 저장되지 않는다 (입력이 일반 읽기면 대기 없이 저장돼 버린다)"""
    for who in ("race1", "race2"):
        order_uuid = _order(base_url, s, who)
        assert_status(requests.post(_ev(base_url, s, f"/orders/{order_uuid}/refuse"), json={"reasonType": "DEPOSIT_UNCONFIRMED"}, headers=_h(s, "manager")), 200)
        order_id = _sql(f"SELECT order_id FROM tbl_order WHERE uuid = '{order_uuid}'")
        holder = subprocess.Popen(
            ["mysql", "-h", "127.0.0.1", "-P", "13306", "-u", "dudoong", "-pdudoong", "--unbuffered", "-N", DB_NAME],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
        )
        holder.stdin.write(f"BEGIN;\nSELECT order_id FROM tbl_order WHERE order_id = {order_id} FOR UPDATE;\nSELECT 'LOCKED';\n")
        holder.stdin.flush()
        while holder.stdout.readline().strip() != "LOCKED":
            pass
        results, errors = {}, {}

        def run(key, call):
            try:
                results[key] = call()
            except Exception as e:
                errors[key] = e

        try:
            t1 = threading.Thread(target=run, args=("complete", lambda: requests.patch(
                f"{base_url}/v1/events/{s.event_id}/refunds/{order_uuid}/complete", headers=_h(s, "master"))))
            t1.start()
            _await_waits(1)
            t2 = threading.Thread(target=run, args=("put", lambda: _put(base_url, s, who, order_uuid)))
            t2.start()
            # 정상: 입력도 주문 행 잠금을 기다린다. 일반 읽기 변형이면 대기 없이 끝나 2건이 되지 않는다
            try:
                _await_waits(2, timeout=3.0)
            except AssertionError:
                pass
        finally:
            holder.stdin.write("COMMIT;\n")
            holder.stdin.close()
            holder.wait(timeout=10)
        t1.join(30)
        t2.join(30)
        for key in ("complete", "put"):
            if key in errors:
                raise errors[key]
        assert results["complete"].status_code == 200, results["complete"].text
        assert results["put"].status_code == 400 and _code(results["put"]) == "Order_400_28", results["put"].text
        assert _sql(f"SELECT COUNT(*) FROM tbl_order_refund_account WHERE order_id = {order_id}") == "0"
