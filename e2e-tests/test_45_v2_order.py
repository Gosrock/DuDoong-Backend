"""
v2 사용자 앱 주문·주문내역·취소/환불 API E2E 테스트 (#718).

시나리오: 호스트·멤버·공연·티켓(두둥 + 옵션 2개, 무료 선착순, 무료 승인) 준비 → v2 등록 →
사용자 v2 주문(계좌이체·토스·티켓별 옵션·무료) → 검증 오류(결제 방식·입금자명·옵션·동의·PG 없음) → 중복 요청 →
호스트 v2 승인 / 거절(사유) → 사용자 주문내역(필터)·상세(거절 사유·계좌) → v1 호환(v1 승인·조회·통계) →
취소·환불 요청(환불 계좌) → 호스트 환불 목록(계좌는 매니저 이상만) → 환불 완료 → 알림 →
동시성(마지막 재고 무료 선착순 / 같은 사용자 중복 요청).

재실행해도 충돌하지 않도록 유저 이메일에 실행마다 다른 접미사를 붙인다. DB 직접 접근은 하지 않는다.
"""
import re
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta
from typing import Optional

import pytest
import requests

from conftest import assert_status, get_data

RUN = uuid.uuid4().hex[:8]
FMT = "%Y.%m.%d %H:%M"
START = (datetime.now() + timedelta(days=30)).replace(hour=18, minute=0, second=0, microsecond=0)
END = START + timedelta(minutes=120)
PLACE = {"name": "롤링홀", "address": "서울 마포구 어울마당로 35", "latitude": 37.548369, "longitude": 126.920036}
SECTIONS = [{"title": "공연 소개", "content": "<p>주문 테스트</p>", "sortOrder": 0}]
ACCOUNT = {"bankName": "신한은행", "accountHolder": "고스락", "accountNumber": "110-123-456789"}
REFUND_ACCOUNT = {"bankName": "국민은행", "accountHolder": "홍길동", "accountNumber": "123-45-678901"}
BUYERS = ["bank", "toss", "split", "free", "refused", "dup", "cancel_pending", "race1", "race2", "race3", "v1buyer"]


class OrderState:
    tokens: dict = {}
    emails: dict = {}
    host_id: int = 0
    event_id: int = 0
    ticket_id: int = 0
    free_id: int = 0
    free_approval_id: int = 0
    race_id: int = 0
    limit_id: int = 0
    yes_no_id: int = 0
    subjective_id: int = 0
    orders: dict = {}


@pytest.fixture(scope="module")
def s():
    return OrderState()


def _f(dt):
    return dt.strftime(FMT)


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


def _answers(s, yes=True, text="홍길동"):
    return [{"optionId": s.yes_no_id, "answer": "YES" if yes else "NO"}, {"optionId": s.subjective_id, "answer": text}]


def _body(
    s,
    ticket_id: Optional[int] = None,
    quantity: int = 1,
    answers: Optional[list] = None,
    per_ticket: Optional[list] = None,
    method: str = "BANK_TRANSFER",
    depositor: Optional[str] = "입금자",
    agree: Optional[bool] = True,
):
    return {
        "eventId": s.event_id,
        "ticketItemId": ticket_id or s.ticket_id,
        "quantity": quantity,
        "options": {"applyToAll": per_ticket is None, "answers": answers if answers is not None else []},
        "perTicketOptions": per_ticket,
        "paymentChannel": method,
        "depositorName": depositor,
        "agreeRefundPolicy": agree,
    }


def _order(base_url, s, who, body):
    return requests.post(f"{base_url}/v2/me/orders", json=body, headers=_h(s, who))


def _order_ok(base_url, s, who, body):
    resp = _order(base_url, s, who, body)
    assert_status(resp, 200)
    return get_data(resp)


def _mine(base_url, s, who, **params):
    resp = requests.get(f"{base_url}/v2/me/orders", params=params, headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)


def _detail(base_url, s, who, order_uuid):
    resp = requests.get(f"{base_url}/v2/me/orders/{order_uuid}", headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)


def _cancel(base_url, s, who, order_uuid, account=None):
    return requests.post(f"{base_url}/v2/me/orders/{order_uuid}/cancel", json={"refundAccount": account}, headers=_h(s, who))


def _ev(base_url, s, path):
    return f"{base_url}/v2/events/{s.event_id}{path}"


def _host_detail(base_url, s, who, order_uuid):
    resp = requests.get(_ev(base_url, s, f"/orders/{order_uuid}"), headers=_h(s, who))
    assert_status(resp, 200)
    return get_data(resp)


def _ticket(base_url, s, body):
    resp = requests.post(_ev(base_url, s, "/ticket-items"), json=body, headers=_h(s, "manager"))
    assert_status(resp, 200)
    return get_data(resp)["ticketItemId"]


def _free_body(name, supply, approval):
    return {"payType": "FREE", "name": name, "description": "무료", "price": 0, "supplyCount": supply, "account": None,
            "approvalRequired": approval, "isQuantityPublic": True, "purchaseLimit": 2, "saleStartAt": None, "saleEndAt": None}


def _wait_notification(base_url, s, who, type_, target):
    deadline = time.time() + 10
    while time.time() < deadline:
        resp = requests.get(f"{base_url}/v2/me/notifications", params={"size": 100}, headers=_h(s, who))
        assert_status(resp, 200)
        found = [n for n in get_data(resp)["content"] if n["type"] == type_ and n["target"]["targetId"] == target]
        if found:
            return found
        time.sleep(0.2)
    return []


def test_01_setup(base_url, s):
    people = [("master", "주문마스터"), ("manager", "주문매니저"), ("guest", "주문일반"), ("outsider", "주문외부")]
    people += [(b, f"주문구매{i}") for i, b in enumerate(BUYERS)]
    for who, name in people:
        email = f"v2order-{who}-{RUN}@dudoong.com"
        s.tokens[who], s.emails[who] = _login(base_url, email, name), email

    resp = requests.post(f"{base_url}/v2/hosts", json={"name": f"주문{RUN[:5]}", "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    s.host_id = get_data(resp)["hostId"]
    resp = requests.post(
        f"{base_url}/v2/hosts/{s.host_id}/members",
        json={"members": [{"email": s.emails["manager"], "role": "MANAGER"}, {"email": s.emails["guest"], "role": "GUEST"}]},
        headers=_h(s, "master"),
    )
    assert_status(resp, 200)
    resp = requests.post(
        f"{base_url}/v2/events",
        json={"hostId": s.host_id, "name": "v2주문공연", "startAt": _f(START), "endAt": _f(END), "hasTicket": True},
        headers=_h(s, "manager"),
    )
    assert_status(resp, 200)
    s.event_id = get_data(resp)["eventId"]
    s.ticket_id = _ticket(base_url, s, {
        "payType": "DUDOONG", "name": "일반", "description": "일반 입장", "price": 6000, "supplyCount": 30, "account": ACCOUNT,
        "approvalRequired": True, "isQuantityPublic": True, "purchaseLimit": 4, "saleStartAt": None, "saleEndAt": None,
    })
    option_ids = []
    for body in [{"name": "뒷풀이", "description": "참석?", "type": "YES_NO", "yesAdditionalPrice": 1000}, {"name": "요청사항", "description": "요청", "type": "SUBJECTIVE"}]:
        resp = requests.post(_ev(base_url, s, "/options"), json=body, headers=_h(s, "manager"))
        assert_status(resp, 200)
        option_ids.append(get_data(resp)["optionId"])
    s.yes_no_id, s.subjective_id = option_ids
    assert_status(requests.put(_ev(base_url, s, f"/ticket-items/{s.ticket_id}/options"), json={"optionIds": option_ids}, headers=_h(s, "manager")), 200)
    s.free_id = _ticket(base_url, s, _free_body("무료선착순", 10, False))
    s.free_approval_id = _ticket(base_url, s, _free_body("무료승인", 10, True))
    s.race_id = _ticket(base_url, s, _free_body("마지막한장", 1, False))
    s.limit_id = _ticket(base_url, s, _free_body("1인2장", 10, False))
    assert_status(requests.put(_ev(base_url, s, f"/ticket-items/{s.limit_id}/options"), json={"optionIds": [s.subjective_id]}, headers=_h(s, "manager")), 200)

    key = get_data(requests.post(_ev(base_url, s, "/images"), json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "manager")))["key"]
    resp = requests.patch(_ev(base_url, s, "/basic"), json={"posterImageKey": key, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert_status(requests.put(_ev(base_url, s, "/sections"), json={"sections": SECTIONS}, headers=_h(s, "manager")), 200)
    assert_status(requests.post(_ev(base_url, s, "/open"), headers=_h(s, "manager")), 200)

    # 공개 티켓 목록(P-5): 세 티켓 모두 구매 가능
    tickets = get_data(requests.get(_ev(base_url, s, "/ticket-items")))
    assert len(tickets) == 5 and all(t["isPurchasable"] for t in tickets)


def test_02_create_orders(base_url, s):
    # 계좌이체: 승인 대기, 입금자명 trim, 금액 = (6000 + 1000) x 2, 계좌 제공
    data = _order_ok(base_url, s, "bank", _body(s, quantity=2, answers=_answers(s), depositor="  128구구 "))
    assert data["status"] == "PENDING_APPROVE" and data["paymentChannel"] == "BANK_TRANSFER" and data["depositorName"] == "128구구"
    assert data["payment"]["ticketAmount"] == 12000 and data["payment"]["optionAmount"] == 2000 and data["payment"]["totalAmount"] == 14000
    assert data["payment"]["account"] == {"bankName": "신한은행", "accountHolder": "고스락", "accountNumber": "110-123-456789"}
    assert data["canCancel"] is True and data["orderNo"].startswith("R")
    s.orders["bank"] = data["orderUuid"]

    data = _order_ok(base_url, s, "toss", _body(s, answers=_answers(s, yes=False), method="TOSS_TRANSFER"))
    assert data["paymentChannel"] == "TOSS_TRANSFER" and data["payment"]["totalAmount"] == 6000
    s.orders["toss"] = data["orderUuid"]

    # 티켓별 옵션: 수량 1 라인 2개
    data = _order_ok(base_url, s, "split", _body(s, quantity=2, per_ticket=[_answers(s, True, "첫째"), _answers(s, False, "둘째")]))
    assert [line["quantity"] for line in data["lines"]] == [1, 1]
    assert data["payment"]["totalAmount"] == 13000
    s.orders["split"] = data["orderUuid"]

    # 무료 선착순: 즉시 발급 / 무료 승인: 승인 대기
    data = _order_ok(base_url, s, "free", _body(s, ticket_id=s.free_id, quantity=2, method="FREE", depositor=None))
    assert data["status"] == "APPROVED" and data["payment"] is None and len(data["issuedTickets"]) == 2
    s.orders["free"] = data["orderUuid"]
    data = _order_ok(base_url, s, "free", _body(s, ticket_id=s.free_approval_id, method="FREE", depositor=None))
    assert data["status"] == "PENDING_APPROVE" and data["issuedTickets"] == []
    s.orders["free_approval"] = data["orderUuid"]

    s.orders["refused"] = _order_ok(base_url, s, "refused", _body(s, answers=_answers(s)))["orderUuid"]
    s.orders["cancel_pending"] = _order_ok(base_url, s, "cancel_pending", _body(s, answers=_answers(s)))["orderUuid"]

    # 호스트 목록에 입금자명·결제 방식
    resp = requests.get(_ev(base_url, s, "/orders"), params={"status": "PENDING_APPROVE"}, headers=_h(s, "guest"))
    assert_status(resp, 200)
    bank = next(o for o in get_data(resp)["orders"]["content"] if o["orderUuid"] == s.orders["bank"])
    assert bank["depositorName"] == "128구구" and bank["paymentChannel"] == "BANK_TRANSFER"


def test_03_validation_errors(base_url, s):
    cases = [
        (_body(s, quantity=2, per_ticket=[_answers(s)]), "Order_400_23"),
        (_body(s, answers=[{"optionId": s.yes_no_id, "answer": "YES"}]), "Order_400_23"),
        (_body(s, answers=[{"optionId": s.yes_no_id, "answer": "MAYBE"}, {"optionId": s.subjective_id, "answer": "x"}]), "Order_400_23"),
        (_body(s, answers=_answers(s), method="FREE"), "Order_400_21"),
        (_body(s, ticket_id=s.free_id, method="BANK_TRANSFER"), "Order_400_21"),
        (_body(s, answers=_answers(s), depositor="   "), "Order_400_22"),
        (_body(s, answers=_answers(s), depositor="가" * 21), "Order_400_22"),
        (_body(s, answers=_answers(s), quantity=5), "Ticket_Item_400_6"),
    ]
    for body, code in cases:
        resp = _order(base_url, s, "dup", body)
        assert_status(resp, 400)
        assert _code(resp) == code, (body, resp.text[:300])
    assert_status(_order(base_url, s, "dup", _body(s, answers=_answers(s), agree=False)), 400)
    assert_status(_order(base_url, s, "dup", _body(s, answers=_answers(s), agree=None)), 400)
    assert_status(requests.post(f"{base_url}/v2/me/orders", json=_body(s, answers=_answers(s))), 401)
    assert _mine(base_url, s, "dup")["content"] == []


def test_04_duplicate_request(base_url, s):
    first = _order_ok(base_url, s, "dup", _body(s, answers=_answers(s)))["orderUuid"]
    again = _order_ok(base_url, s, "dup", _body(s, answers=_answers(s)))["orderUuid"]
    assert first == again
    other = _order_ok(base_url, s, "dup", _body(s, answers=_answers(s, yes=False)))["orderUuid"]
    assert other != first
    assert len(_mine(base_url, s, "dup")["content"]) == 2
    s.orders["dup"] = first


def test_05_host_approve_refuse(base_url, s):
    o = s.orders
    for key in ("bank", "split"):
        resp = requests.post(_ev(base_url, s, f"/orders/{o[key]}/approve"), headers=_h(s, "manager"))
        assert_status(resp, 200)
        assert get_data(resp)["order"]["status"] == "APPROVED"
    # 티켓별 옵션 → 발급 티켓마다 자기 답변
    detail = _host_detail(base_url, s, "guest", o["split"])
    answers = sorted(tuple(a["answer"] for a in t["optionAnswers"]) for t in detail["issuedTickets"])
    assert answers == [("아니요", "둘째"), ("예", "첫째")]

    resp = requests.post(_ev(base_url, s, f"/orders/{o['refused']}/refuse"), json={"reasonType": "DEPOSIT_UNCONFIRMED"}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    # v1 호스트 API 로 v2 주문 승인 (호환)
    assert_status(requests.post(f"{base_url}/v1/events/{s.event_id}/orders/{o['toss']}/approve", headers=_h(s, "master")), 200)


def test_06_my_orders_and_detail(base_url, s):
    o = s.orders
    d = _detail(base_url, s, "refused", o["refused"])
    assert d["status"] == "REFUSED" and d["refuseReasonType"] == "DEPOSIT_UNCONFIRMED" and d["refuseReason"] == "입금 미확인"
    assert d["canCancel"] is False

    d = _detail(base_url, s, "toss", o["toss"])
    assert d["status"] == "APPROVED" and len(d["issuedTickets"]) == 1 and d["canCancel"] is True

    # 남의 주문 404
    assert_status(requests.get(f"{base_url}/v2/me/orders/{o['bank']}", headers=_h(s, "toss")), 404)
    assert_status(requests.get(f"{base_url}/v2/me/orders/{o['bank']}", headers=_h(s, "master")), 404)

    # 필터
    free_all = _mine(base_url, s, "free")
    assert [x["orderUuid"] for x in free_all["content"]] == [o["free_approval"], o["free"]]
    assert free_all["content"][0]["event"]["eventId"] == s.event_id and free_all["content"][0]["event"]["displayStatus"] == "UPCOMING"
    assert [x["orderUuid"] for x in _mine(base_url, s, "free", status="APPROVED")["content"]] == [o["free"]]
    assert [x["orderUuid"] for x in _mine(base_url, s, "free", status="PENDING_APPROVE")["content"]] == [o["free_approval"]]
    assert _mine(base_url, s, "free", status="REFUSED")["content"] == []
    page = _mine(base_url, s, "free", page=1, size=1)
    assert page["totalElements"] == 2 and page["totalPages"] == 2 and [x["orderUuid"] for x in page["content"]] == [o["free"]]

    # v1 사용자 주문이 섞여도 보임 (v1 카트 → 주문)
    cart = requests.post(f"{base_url}/v1/carts", json={"items": [{"itemId": s.free_approval_id, "quantity": 1, "options": []}]}, headers=_h(s, "v1buyer"))
    assert cart.status_code in (200, 201), cart.text[:300]
    resp = requests.post(f"{base_url}/v1/orders/", json={"cartId": get_data(cart)["cartId"], "couponId": None}, headers=_h(s, "v1buyer"))
    assert resp.status_code in (200, 201), resp.text[:300]
    v1_uuid = get_data(resp)["orderId"]
    mine = _mine(base_url, s, "v1buyer")["content"]
    assert [x["orderUuid"] for x in mine] == [v1_uuid] and mine[0]["status"] == "PENDING_APPROVE"
    assert _detail(base_url, s, "v1buyer", v1_uuid)["paymentChannel"] is None
    # v1 사용자 앱에서 v2 주문 조회
    resp = requests.get(f"{base_url}/v1/orders/{o['bank']}", headers=_h(s, "bank"))
    assert_status(resp, 200)
    assert get_data(resp)["orderUuid"] == o["bank"]
    resp = requests.get(f"{base_url}/v1/orders", params={"showing": "true"}, headers=_h(s, "bank"))
    assert_status(resp, 200)
    assert o["bank"] in str(get_data(resp))


def test_07_v1_statistics_match(base_url, s):
    # 승인 완료: bank 14000 + split 13000 + toss 6000 (+ 무료 0)
    resp = requests.get(_ev(base_url, s, "/dashboard"), headers=_h(s, "guest"))
    assert_status(resp, 200)
    assert get_data(resp)["salesAmount"] == 33000
    resp = requests.get(f"{base_url}/v1/events/{s.event_id}/statistics", headers=_h(s, "master"))
    assert_status(resp, 200)
    sell = get_data(resp)["sellAmount"]
    assert int(re.sub(r"[^0-9]", "", str(sell))) == 33000, sell


def test_08_cancel(base_url, s):
    o = s.orders
    # 승인 대기 유료: 계좌 필수
    resp = _cancel(base_url, s, "cancel_pending", o["cancel_pending"])
    assert_status(resp, 400)
    assert _code(resp) == "Order_400_25"
    assert_status(_cancel(base_url, s, "toss", o["cancel_pending"], REFUND_ACCOUNT), 404)
    resp = _cancel(base_url, s, "cancel_pending", o["cancel_pending"], REFUND_ACCOUNT)
    assert_status(resp, 200)
    d = get_data(resp)
    assert d["status"] == "REFUNDED" and d["refundStatus"] == "REQUESTED"
    assert d["refundAccount"] == {"bankName": "국민은행", "accountHolder": "홍길동", "maskedAccountNumber": "*********8901"}
    resp = _cancel(base_url, s, "cancel_pending", o["cancel_pending"], REFUND_ACCOUNT)
    assert_status(resp, 400)
    assert _code(resp) == "Order_400_5"

    # 승인 완료 유료: 입장 후 불가
    tickets = _detail(base_url, s, "bank", o["bank"])["issuedTickets"]
    resp = requests.post(_ev(base_url, s, "/check-ins"), json={"ticketUuid": tickets[0]["ticketUuid"]}, headers=_h(s, "guest"))
    assert_status(resp, 200)
    resp = _cancel(base_url, s, "bank", o["bank"], REFUND_ACCOUNT)
    assert_status(resp, 400)
    assert _code(resp) == "Order_400_24"

    # 승인 완료 유료(입장 전): v1 사용자 환불과 같은 전이, 티켓 취소
    resp = _cancel(base_url, s, "toss", o["toss"], REFUND_ACCOUNT)
    assert_status(resp, 200)
    d = get_data(resp)
    assert d["status"] == "REFUNDED" and {t["entrance"] for t in d["issuedTickets"]} == {"CANCELED"}

    # 무료: 계좌 없이 취소, 환불 요청 없음
    resp = _cancel(base_url, s, "free", o["free"])
    assert_status(resp, 200)
    d = get_data(resp)
    assert d["status"] == "CANCELED" and d["refundStatus"] == "NONE"

    # 호스트 분류: 사용자 취소는 CANCELED (거절 아님)
    assert _host_detail(base_url, s, "manager", o["cancel_pending"])["order"]["status"] == "CANCELED"


def test_09_host_refund_account_scope(base_url, s):
    o = s.orders
    for who in ("master", "manager"):
        assert _host_detail(base_url, s, who, o["cancel_pending"])["refundAccount"]["accountNumber"] == "123-45-678901"
        resp = requests.get(_ev(base_url, s, "/refunds"), params={"status": "REQUESTED"}, headers=_h(s, who))
        assert_status(resp, 200)
        row = next(r for r in get_data(resp)["content"] if r["orderUuid"] == o["cancel_pending"])
        assert row["refundAccount"]["accountHolder"] == "홍길동"
    assert _host_detail(base_url, s, "guest", o["cancel_pending"])["refundAccount"] is None
    resp = requests.get(_ev(base_url, s, "/refunds"), headers=_h(s, "guest"))
    assert_status(resp, 200)
    assert all(r["refundAccount"] is None for r in get_data(resp)["content"])
    assert_status(requests.get(_ev(base_url, s, f"/orders/{o['cancel_pending']}"), headers=_h(s, "outsider")), 403)

    resp = requests.post(_ev(base_url, s, f"/refunds/{o['cancel_pending']}/complete"), headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert get_data(resp)["order"]["refundStatus"] == "COMPLETED"
    d = _detail(base_url, s, "cancel_pending", o["cancel_pending"])
    assert d["status"] == "REFUNDED" and d["refundStatus"] == "COMPLETED"
    assert [x["orderUuid"] for x in _mine(base_url, s, "cancel_pending", status="REFUNDED")["content"]] == [o["cancel_pending"]]


def test_10_notifications(base_url, s):
    o = s.orders
    for who in ("master", "manager"):
        assert _wait_notification(base_url, s, who, "ORDER_REFUND_REQUESTED", o["cancel_pending"]), who
        assert _wait_notification(base_url, s, who, "ORDER_CANCELED_BY_USER", o["free"]), who
    resp = requests.get(f"{base_url}/v2/me/notifications", params={"size": 100}, headers=_h(s, "guest"))
    assert_status(resp, 200)
    assert not [n for n in get_data(resp)["content"] if n["type"] in ("ORDER_REFUND_REQUESTED", "ORDER_CANCELED_BY_USER")]
    # 주문자: 승인·거절 알림 (5단계)
    assert _wait_notification(base_url, s, "refused", "ORDER_REFUSED", o["refused"])


def _concurrently(fns):
    """fns 를 동시에 실행하고 결과를 순서대로 돌려준다. 작업 중 예외는 future.result() 로 그대로 올라온다"""
    with ThreadPoolExecutor(max_workers=len(fns)) as pool:
        futures = [pool.submit(fn) for fn in fns]
        return [f.result(timeout=60) for f in futures]


def test_11_concurrency(base_url, s):
    # 마지막 재고 1장 무료 선착순에 3명 동시 주문 → 1명만 발급, 나머지는 재고 부족 400 (500 없음)
    racers = ["race1", "race2", "race3"]
    results = _concurrently([lambda who=who: _order(base_url, s, who, _body(s, ticket_id=s.race_id, method="FREE", depositor=None)) for who in racers])
    assert sorted(r.status_code for r in results) == [200, 400, 400], [r.text[:200] for r in results]
    assert [_code(r) for r in results if r.status_code != 200] == ["Ticket_Item_400_1", "Ticket_Item_400_1"], [r.text[:200] for r in results]
    approved = [who for who in racers if any(x["status"] == "APPROVED" for x in _mine(base_url, s, who)["content"])]
    assert len(approved) == 1
    tickets = get_data(requests.get(_ev(base_url, s, "/ticket-items")))
    race = next(t for t in tickets if t["ticketItemId"] == s.race_id)
    assert race["isSoldOut"] is True

    # 같은 사용자 같은 요청 동시 3번 → 주문 1건
    results = _concurrently([lambda: _order(base_url, s, "race1", _body(s, answers=_answers(s), depositor="동시")) for _ in range(3)])
    ok = [get_data(r)["orderUuid"] for r in results if r.status_code == 200]
    assert len(set(ok)) == 1, [r.text[:200] for r in results]
    assert len([x for x in _mine(base_url, s, "race1")["content"] if x["ticketName"] == "일반"]) == 1


def test_12_purchase_limit_concurrency(base_url, s):
    # 무료 선착순 1인 2장: 같은 사용자가 옵션 답변만 다른 3요청(중복 판정 안 됨)을 동시에 → 발급은 2장을 넘지 않고, 실패는 1인 제한 400
    bodies = [_body(s, ticket_id=s.limit_id, answers=[{"optionId": s.subjective_id, "answer": f"답{i}"}], method="FREE", depositor=None) for i in range(3)]
    results = _concurrently([lambda b=b: _order(base_url, s, "race2", b) for b in bodies])
    assert all(r.status_code in (200, 400) for r in results), [r.text[:200] for r in results]
    assert all(_code(r) == "Ticket_Item_400_6" for r in results if r.status_code == 400), [r.text[:200] for r in results]
    mine = [x for x in _mine(base_url, s, "race2")["content"] if x["ticketName"] == "1인2장"]
    issued = sum(x["quantity"] for x in mine if x["status"] == "APPROVED")
    assert 1 <= issued <= 2 and issued == sum(1 for r in results if r.status_code == 200), (issued, [r.text[:200] for r in results])
    # 제한까지 다 찼으면 추가 주문은 1인 제한
    if issued == 2:
        resp = _order(base_url, s, "race2", _body(s, ticket_id=s.limit_id, answers=[{"optionId": s.subjective_id, "answer": "추가"}], method="FREE", depositor=None))
        assert_status(resp, 400)
        assert _code(resp) == "Ticket_Item_400_6"
