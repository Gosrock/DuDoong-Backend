"""
v1·v2 주문 생성 락 통일 E2E (#724). v1 주문 생성(`주문생성:{userId}` 락)과 v2 주문 생성(`티켓관리:{ticketItemId}` 락)이 서로 다른 락이라
같은 티켓에 v1·v2 주문이 동시에 들어오면 1인 제한·승인 대기 재고 검사가 줄 서지 않았다 → v1 도 `티켓관리:{ticketItemId}` 락 안에서 만든다.

결정적 경합: 별도 세션이 tbl_order 의 끝 간격(마지막 order_id 뒤)을 FOR UPDATE 로 잡아 두면 새 주문 INSERT 가 검사를 마친 뒤 그 자리에서 기다린다.
먼저 보낸 주문이 INSERT 를 기다리는 동안 티켓 락(Redis)이 잡혀 있는지 보고, 나중 주문을 보낸 뒤 풀어 결과를 본다.
- 고친 뒤: 나중 주문은 티켓 락을 기다렸다가 먼저 주문을 보고 거부된다 (1인 제한 Order_400_15 / 재고 Ticket_Item_400_1)
- 예전: 나중 주문도 검사를 통과해 INSERT 에서 기다리고, 풀리면 둘 다 만들어져 한도를 넘는다

performance_schema(root, conftest e2e_db_root)·Redis(REDIS_HOST/REDIS_PORT, 기본 127.0.0.1:6379)가 필요하다 — 못 읽으면 skip, E2E_REQUIRE_LOCK_INSPECTION=1 이면 실패.
"""
import base64
import json
import os
import shutil
import socket
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
SECTIONS = [{"title": "공연 소개", "content": "<p>주문 락 테스트</p>", "sortOrder": 0}]
ACCOUNT = {"bank": "신한은행", "holder": "고스락", "number": "110-123-456789"}
ROUNDS = 2
PEOPLE = ["master"] + [f"limit{i}" for i in range(ROUNDS)] + [f"a{i}" for i in range(ROUNDS)] + [f"b{i}" for i in range(ROUNDS)]
TICKET_LOCK = "티켓관리"
REDIS_HOST = os.environ.get("REDIS_HOST", "127.0.0.1")
REDIS_PORT = int(os.environ.get("REDIS_PORT", "6379"))


class State:
    tokens: dict = {}
    event_id: int = 0
    tickets: dict = {}


@pytest.fixture(scope="module")
def s():
    return State()


# conftest e2e_db fixture 를 이 모듈의 헬퍼가 쓰도록 묶는다 (#737)
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


def _login(base_url, email, name):
    resp = requests.post(f"{base_url}/v1/auth/oauth/local/login", json={
        "email": email, "name": name, "phoneNumber": "010-0000-0000", "profileImage": None, "marketingAgree": False,
    })
    assert_status(resp, 200)
    return get_data(resp)["accessToken"]


def _ev(base_url, s, path=""):
    return f"{base_url}/v2/events/{s.event_id}{path}"


def _ticket(base_url, s, key, supply, limit):
    resp = requests.post(_ev(base_url, s, "/ticket-items"), json={
        "payType": "DUDOONG", "name": key, "description": key, "price": 6000, "supplyCount": supply, "account": ACCOUNT,
        "approvalRequired": True, "isQuantityPublic": True, "purchaseLimit": limit, "saleStartAt": None, "saleEndAt": None,
    }, headers=_h(s, "master"))
    assert_status(resp, 200)
    s.tickets[key] = get_data(resp)["ticketItemId"]


def _v1_order(base_url, s, who, key, quantity):
    """v1 앱 주문: 장바구니(사용자당 1개) → 주문"""
    cart = requests.post(f"{base_url}/v1/carts", json={"items": [{"itemId": s.tickets[key], "quantity": quantity, "options": []}]}, headers=_h(s, who))
    assert_status(cart, 200)
    return requests.post(f"{base_url}/v1/orders/", json={"cartId": get_data(cart)["cartId"], "couponId": None}, headers=_h(s, who))


def _v2_order(base_url, s, who, key, quantity):
    return requests.post(f"{base_url}/v2/orders", json={
        "eventId": s.event_id, "ticketItemId": s.tickets[key], "quantity": quantity,
        "options": {"applyToAll": True, "answers": []}, "perTicketOptions": None,
        "paymentMethod": "BANK_TRANSFER", "depositorName": f"입금{uuid.uuid4().hex[:4]}", "agreeRefundPolicy": True,
    }, headers=_h(s, who))


def _pending(ticket_id, user=None):
    where = f"l.item_id = {ticket_id} AND o.order_status = 'PENDING_APPROVE'"
    if user:
        where += f" AND o.user_id = (SELECT user_id FROM tbl_user WHERE email = 'lock724-{user}-{RUN}@dudoong.com')"
    return int(_sql(f"SELECT IFNULL(SUM(l.quantity), 0) FROM tbl_order o JOIN tbl_order_line l ON l.order_id = o.order_id WHERE {where}"))


def test_01_setup(base_url, s):
    for who in PEOPLE:
        s.tokens[who] = _login(base_url, f"lock724-{who}-{RUN}@dudoong.com", f"락{who}")
    resp = requests.post(f"{base_url}/v2/hosts", json={"name": f"락{RUN[:5]}", "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    host_id = get_data(resp)["hostId"]
    resp = requests.post(f"{base_url}/v2/events", json={
        "hostId": host_id, "name": "주문락공연", "startAt": START.strftime(FMT), "endAt": END.strftime(FMT), "hasTicket": True}, headers=_h(s, "master"))
    assert_status(resp, 200)
    s.event_id = get_data(resp)["eventId"]
    for i in range(ROUNDS):
        _ticket(base_url, s, f"limit{i}", supply=50, limit=4)
        _ticket(base_url, s, f"stock{i}", supply=3, limit=10)
    key = get_data(requests.post(_ev(base_url, s, "/images"), json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "master")))["key"]
    assert_status(requests.patch(_ev(base_url, s, "/basic"), json={"posterImageKey": key, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]}, headers=_h(s, "master")), 200)
    assert_status(requests.put(_ev(base_url, s, "/sections"), json=SECTIONS, headers=_h(s, "master")), 200)
    assert_status(requests.post(_ev(base_url, s, "/open"), headers=_h(s, "master")), 200)


# ===== MySQL 경합 (결정적) =====

needs_lock_inspection = pytest.mark.usefixtures("e2e_db_root")


def _lock_waits(table):
    return int(DB.query(
        "SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks l "
        f"ON l.ENGINE_LOCK_ID = w.REQUESTING_ENGINE_LOCK_ID WHERE l.OBJECT_SCHEMA = '{DB.name}' AND l.OBJECT_NAME = '{table}'",
        root=True, database=False,
    ))


def _await_waits(n, table, timeout=6.0):
    deadline = time.time() + timeout
    while time.time() < deadline:
        if _lock_waits(table) >= n:
            return
        time.sleep(0.05)
    raise AssertionError(f"{table} 잠금 대기 {n}건이 안 됨 (현재 {_lock_waits(table)})")


class _RowLock:
    """별도 세션에서 BEGIN; <잠금 SELECT>; 를 실행해 둔 채로 있다가 release() 에서 COMMIT. 세션 잠금 대기는 10초 상한"""

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


def _redis_exists(key):
    """EXISTS key. redis-py → redis-cli -h -p → 소켓(RESP) 순. 접속 실패는 ConnectionError (test_25 와 같은 방식)"""
    try:
        import redis  # 선택 의존성
    except ImportError:
        redis = None
    if redis is not None:
        try:
            return redis.Redis(host=REDIS_HOST, port=REDIS_PORT, socket_timeout=5).exists(key)
        except redis.RedisError as e:
            raise ConnectionError(e) from e
    if shutil.which("redis-cli"):
        out = subprocess.run(["redis-cli", "-h", REDIS_HOST, "-p", str(REDIS_PORT), "EXISTS", key], capture_output=True, text=True, timeout=10)
        if out.returncode != 0 or not out.stdout.strip().isdigit():
            raise ConnectionError(out.stderr or out.stdout)
        return int(out.stdout.strip())
    raw = key.encode()
    with socket.create_connection((REDIS_HOST, REDIS_PORT), timeout=5) as conn:
        conn.sendall(b"*2\r\n$6\r\nEXISTS\r\n$" + str(len(raw)).encode() + b"\r\n" + raw + b"\r\n")
        reply = conn.recv(64)
    if not reply.startswith(b":"):
        raise ConnectionError(reply)
    return int(reply[1:].split(b"\r\n")[0])


@pytest.fixture(scope="module")
def redis_lock_inspection():
    try:
        _redis_exists("e2e-redis-ping")
    except (OSError, ConnectionError, subprocess.SubprocessError) as e:
        reason = f"Redis({REDIS_HOST}:{REDIS_PORT})에 접속할 수 없음: {e}"
        if os.environ.get("E2E_REQUIRE_LOCK_INSPECTION") == "1":
            pytest.fail(f"{reason} — E2E_REQUIRE_LOCK_INSPECTION=1")
        pytest.skip(reason)


def _contend(ticket_id, first, second):
    """tbl_order 끝 간격을 잡아 둔 채 first → (first 가 INSERT 대기 + 티켓 락 보유 확인) → second → 해제. (first, second, 티켓 락 보유) 반환"""
    last = int(_sql("SELECT IFNULL(MAX(order_id), 0) FROM tbl_order"))
    results, errors = {}, {}

    def run(key, call):
        try:
            results[key] = call()
        except Exception as e:  # 요청 스레드의 예외를 그대로 다시 던진다
            errors[key] = e

    holder = _RowLock(f"SELECT order_id FROM tbl_order WHERE order_id > {last} FOR UPDATE")
    try:
        t1 = threading.Thread(target=run, args=(0, first))
        t1.start()
        _await_waits(1, "tbl_order")
        held = _redis_exists(f"{TICKET_LOCK}:{ticket_id}") == 1
        t2 = threading.Thread(target=run, args=(1, second))
        t2.start()
        # 예전 코드: second 도 검사를 통과해 INSERT 에서 기다린다(대기 2). 고친 코드: second 는 티켓 락(Redis)을 기다려 대기가 1 로 남는다 — 단언하지 않고 시간만 준다
        try:
            _await_waits(2, "tbl_order", timeout=2.0)
        except AssertionError:
            pass
    finally:
        holder.release()
    t1.join(30)
    t2.join(30)
    for i in (0, 1):
        if i in errors:
            raise errors[i]
        assert i in results, f"요청 {i} 가 30초 안에 끝나지 않음"
    return results[0], results[1], held


@needs_lock_inspection
@pytest.mark.usefixtures("redis_lock_inspection")
def test_02_race_purchase_limit_v1_v2_same_user(base_url, s):
    """같은 사용자 v1(3장) ↔ v2(2장), 1인 제한 4: 먼저 들어간 주문만 만들어지고 나중 주문은 Order_400_15. 회차마다 v1 먼저 / v2 먼저를 바꾼다"""
    for i in range(ROUNDS):
        key, who = f"limit{i}", f"limit{i}"
        v1 = lambda: _v1_order(base_url, s, who, key, 3)
        v2 = lambda: _v2_order(base_url, s, who, key, 2)
        v1_first = i % 2 == 0
        first, second, held = _contend(s.tickets[key], v1 if v1_first else v2, v2 if v1_first else v1)
        pending = _pending(s.tickets[key], who)
        assert_status(first, 200)
        assert second.status_code == 400 and _code(second) == "Order_400_15", (f"v1 먼저={v1_first} 승인 대기 합계 {pending} (1인 제한 4)", second.text)
        assert pending == (3 if v1_first else 2), "승인 대기 합계는 1인 제한 이내"
        assert held, "먼저 들어간 주문이 INSERT 를 기다리는 동안 티켓 락이 잡혀 있어야 한다 (v1·v2 같은 락)"


@needs_lock_inspection
@pytest.mark.usefixtures("redis_lock_inspection")
def test_03_race_pending_stock_v1_v2_other_users(base_url, s):
    """다른 사용자 v1(2장) ↔ v2(2장), 재고 3: 승인 대기 합계가 재고를 넘지 않는다 (나중 주문 Ticket_Item_400_1). 회차마다 순서를 바꾼다"""
    for i in range(ROUNDS):
        key = f"stock{i}"
        v1 = lambda: _v1_order(base_url, s, f"a{i}", key, 2)
        v2 = lambda: _v2_order(base_url, s, f"b{i}", key, 2)
        v1_first = i % 2 == 0
        first, second, held = _contend(s.tickets[key], v1 if v1_first else v2, v2 if v1_first else v1)
        pending = _pending(s.tickets[key])
        assert_status(first, 200)
        assert second.status_code == 400 and _code(second) == "Ticket_Item_400_1", (f"v1 먼저={v1_first} 승인 대기 합계 {pending} (재고 3)", second.text)
        assert pending == 2, "승인 대기 합계 ≤ 재고 3"
        assert held, "먼저 들어간 주문이 INSERT 를 기다리는 동안 티켓 락이 잡혀 있어야 한다"
