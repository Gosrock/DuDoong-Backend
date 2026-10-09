"""
v1 쿠폰 E2E (#746). 쿠폰 캠페인 생성(SUPER_ADMIN — conftest e2e_db.account_role 로 잠시 승격) → 발급(재고 감소·중복 거부) →
쿠폰 적용 주문(유료 선착순 티켓, 제휴 호스트 — 쿠폰 사용 락 키) → MySQL 결정적 경합(발급 재고 감소가 발급 락 안에서 커밋되는지).

예전에는 SUPER_ADMIN 이 없어 캠페인 생성부터 skip 됐고, 쿠폰 사용·회복 락 키가 파라미터 이름과 달라 쿠폰 주문이 늘 500(AOP_500_1)이었다.
결정적 경합(test_03)은 performance_schema(root, conftest e2e_db_root)와 Redis(REDIS_HOST/REDIS_PORT, 기본 127.0.0.1:6379) 가 필요하다 —
못 읽으면 skip, E2E_REQUIRE_LOCK_INSPECTION=1 이면 실패.
재실행해도 충돌하지 않도록 유저 이메일·쿠폰 코드에 실행마다 다른 접미사를 붙인다.
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
SECTIONS = [{"title": "공연 소개", "content": "<p>쿠폰 테스트</p>", "sortOrder": 0}]
PEOPLE = ["admin", "master", "buyer", "race1", "race2"]
ISSUE_LOCK = "유저쿠폰발급"


class CouponState:
    tokens: dict = {}
    user_ids: dict = {}
    emails: dict = {}
    code: str = ""
    campaign_id: int = 0
    issued_coupon_id: int = 0
    event_id: int = 0
    ticket_id: int = 0


@pytest.fixture(scope="module")
def s():
    return CouponState()


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
    token = get_data(resp)["accessToken"]
    part = token.split(".")[1]
    part += "=" * (-len(part) % 4)
    return token, int(json.loads(base64.urlsafe_b64decode(part))["sub"])


def _create_campaign(base_url, s, code, stock):
    """SUPER_ADMIN 으로 잠시 승격해(토큰에 역할이 들어가므로 다시 로그인) 캠페인을 만들고 USER 로 되돌린다"""
    with DB.account_role(s.user_ids["admin"], "SUPER_ADMIN"):
        token, _ = _login(base_url, s.emails["admin"], "쿠폰admin")
        resp = requests.post(f"{base_url}/v1/coupons/campaigns", json={
            "discountType": "AMOUNT", "applyTarget": "ALL", "validTerm": 30,
            "startAt": (datetime.now() - timedelta(minutes=1)).strftime(FMT), "endAt": (datetime.now() + timedelta(days=30)).strftime(FMT),
            "issuedAmount": stock, "discountAmount": 1000, "couponCode": code, "minimumCost": 10000,
        }, headers={"Authorization": f"Bearer {token}"})
    assert_status(resp, 200)
    return int(_sql(f"SELECT coupon_campaign_id FROM tbl_coupon_campaign WHERE coupon_code = '{code}'"))


def _issue(base_url, s, who, code):
    return requests.post(f"{base_url}/v1/coupons/campaigns/{code}", headers=_h(s, who))


def _remaining(campaign_id):
    return int(_sql(f"SELECT remaining_amount FROM tbl_coupon_campaign WHERE coupon_campaign_id = {campaign_id}"))


def test_01_setup_campaign_and_issue(base_url, s):
    for who in PEOPLE:
        email = f"coupon746-{who}-{RUN}@dudoong.com"
        s.tokens[who], s.user_ids[who] = _login(base_url, email, f"쿠폰{who}")
        s.emails[who] = email
    s.code = f"E2E{RUN}"
    s.campaign_id = _create_campaign(base_url, s, s.code, stock=5)
    assert _sql(f"SELECT account_role FROM tbl_user WHERE user_id = {s.user_ids['admin']}") == "USER", "승격은 블록 안에서만"

    resp = _issue(base_url, s, "buyer", s.code)
    assert_status(resp, 200)
    s.issued_coupon_id = get_data(resp)["issuedCouponId"]
    assert _remaining(s.campaign_id) == 4
    assert _issue(base_url, s, "buyer", s.code).status_code == 400, "같은 사람 재발급 거부"
    assert _remaining(s.campaign_id) == 4

    resp = requests.get(f"{base_url}/v1/coupons", params={"expired": "false"}, headers=_h(s, "buyer"))
    assert_status(resp, 200)


def test_02_order_with_coupon_uses_coupon(base_url, s):
    """쿠폰 주문은 v1 유료(PG) 선착순 티켓만 — 제휴 호스트로 만들고 v1 API 로 티켓을 만든다. 주문 생성 때 쿠폰 사용 락(키 = issuedCouponId)"""
    resp = requests.post(f"{base_url}/v2/hosts", json={"name": f"쿠폰{RUN[:5]}", "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    host_id = get_data(resp)["hostId"]
    _sql(f"UPDATE tbl_host SET partner = 1 WHERE host_id = {host_id}")
    resp = requests.post(f"{base_url}/v2/events", json={
        "hostId": host_id, "name": "쿠폰공연", "startAt": START.strftime(FMT), "endAt": END.strftime(FMT), "hasTicket": True}, headers=_h(s, "master"))
    assert_status(resp, 200)
    s.event_id = get_data(resp)["eventId"]
    resp = requests.post(f"{base_url}/v1/events/{s.event_id}/ticketItems", json={
        "payType": "유료티켓", "name": "카드", "description": "카드 결제", "price": 20000, "supplyCount": 10,
        "approveType": "선착순", "isQuantityPublic": True, "purchaseLimit": 4,
    }, headers=_h(s, "master"))
    assert_status(resp, 200)
    s.ticket_id = get_data(resp)["ticketItemId"]
    ev = f"{base_url}/v2/events/{s.event_id}"
    key = get_data(requests.post(f"{ev}/images", json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "master")))["key"]
    assert_status(requests.patch(f"{ev}/basic", json={"posterImageKey": key, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]}, headers=_h(s, "master")), 200)
    assert_status(requests.put(f"{ev}/sections", json={"sections": SECTIONS}, headers=_h(s, "master")), 200)
    assert_status(requests.post(f"{ev}/open", headers=_h(s, "master")), 200)

    cart = requests.post(f"{base_url}/v1/carts", json={"items": [{"itemId": s.ticket_id, "quantity": 1, "options": []}]}, headers=_h(s, "buyer"))
    assert_status(cart, 200)
    resp = requests.post(f"{base_url}/v1/orders/", json={"cartId": get_data(cart)["cartId"], "couponId": s.issued_coupon_id}, headers=_h(s, "buyer"))
    assert_status(resp, 200)
    order_uuid = get_data(resp)["orderId"]
    assert _sql(f"SELECT usage_status + 0 FROM tbl_issued_coupon WHERE issued_coupon_id = {s.issued_coupon_id}") == "1"
    assert _sql(f"SELECT coupon_id FROM tbl_order WHERE uuid = '{order_uuid}'") == str(s.issued_coupon_id)


# ===== MySQL 경합 (결정적) =====

# performance_schema 를 root 로 못 읽으면 결정적 경합 테스트는 skip (conftest e2e_db_root, E2E_DB_ROOT_PASSWORD)
needs_lock_inspection = pytest.mark.usefixtures("e2e_db_root")


def _lock_waits(table):
    """이 DB 의 table 행에 대한 잠금 대기 수 (다른 테이블·다른 DB 의 대기는 세지 않는다)"""
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


REDIS_HOST = os.environ.get("REDIS_HOST", "127.0.0.1")
REDIS_PORT = int(os.environ.get("REDIS_PORT", "6379"))


def _redis_exists(key):
    """EXISTS key (1/0). redis-py 가 있으면 그것으로, 없으면 redis-cli -h -p, 그것도 없으면 RESP 를 소켓으로 직접 보낸다. 접속 실패는 ConnectionError"""
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
    """Redisson 락 키를 볼 수 있는 Redis (REDIS_HOST/REDIS_PORT, 기본 로컬 127.0.0.1:6379 — 서버가 쓰는 Redis 와 같아야 한다).
    접속할 수 없으면 skip, E2E_REQUIRE_LOCK_INSPECTION=1 이면 실패"""
    try:
        _redis_exists("e2e-redis-ping")
    except (OSError, ConnectionError, subprocess.SubprocessError) as e:
        reason = f"Redis({REDIS_HOST}:{REDIS_PORT})에 접속할 수 없음: {e}"
        if os.environ.get("E2E_REQUIRE_LOCK_INSPECTION") == "1":
            pytest.fail(f"{reason} — E2E_REQUIRE_LOCK_INSPECTION=1")
        pytest.skip(reason)


def _redis_lock_held(key):
    """Redisson 락(해시 키)이 지금 잡혀 있는지"""
    return _redis_exists(key) == 1


@needs_lock_inspection
@pytest.mark.usefixtures("redis_lock_inspection")
def test_03_race_issue_stock_decrease_inside_lock(base_url, s):
    """캠페인 행을 별도 세션이 FOR SHARE 로 잡은 채(발급 쿠폰 INSERT 의 FK 확인은 통과, 재고 UPDATE 만 대기) race1 발급 → race2 발급 → 해제.
    - 재고 감소는 발급 락 안에서 커밋돼야 한다: race1 이 재고 UPDATE 를 기다리는 동안 발급 락이 잡혀 있다 (예전: 락 트랜잭션은 INSERT 만 하고 락을 푼 뒤 호출 측 커밋에서 UPDATE)
    - 두 발급 모두 성공하고 재고는 정확히 2 줄어든다 (예전: race2 가 락 밖에서 읽은 옛 재고로 덮어써 1 만 줄었다)"""
    code = f"R{RUN}"
    campaign_id = _create_campaign(base_url, s, code, stock=5)
    results, errors = {}, {}

    def run(key, who):
        try:
            results[key] = _issue(base_url, s, who, code)
        except Exception as e:  # 요청 스레드의 예외를 그대로 다시 던진다
            errors[key] = e

    holder = _RowLock(f"SELECT coupon_campaign_id FROM tbl_coupon_campaign WHERE coupon_campaign_id = {campaign_id} FOR SHARE")
    try:
        t1 = threading.Thread(target=run, args=("race1", "race1"))
        t1.start()
        _await_waits(1, "tbl_coupon_campaign")
        held = _redis_lock_held(f"{ISSUE_LOCK}:{campaign_id}")
        t2 = threading.Thread(target=run, args=("race2", "race2"))
        t2.start()
        time.sleep(0.5)  # race2 가 락 밖 조회(예전 코드)·락 대기에 들어갈 시간
    finally:
        holder.release()
    t1.join(30)
    t2.join(30)
    for key in ("race1", "race2"):
        if key in errors:
            raise errors[key]
        assert key in results, f"{key} 요청이 30초 안에 끝나지 않음"
        assert_status(results[key], 200)
    assert held, "재고 UPDATE 를 기다리는 동안 발급 락이 잡혀 있어야 한다 (재고 감소가 락 트랜잭션 안)"
    assert _remaining(campaign_id) == 3, f"발급 2건 → 재고 3 (현재 {_remaining(campaign_id)})"
    assert _sql(f"SELECT COUNT(*) FROM tbl_issued_coupon WHERE coupon_campaign_id = {campaign_id}") == "2"
