"""
v2 티켓 / 옵션 API E2E 테스트 (#707).

시나리오: 호스트·멤버 준비 → 공연 생성 → 티켓 없는 공연 400 → 생성 검증 실패(PRICE·계좌·판매기간) →
유료(두둥)/무료/무제한 티켓 생성 → 옵션 생성·적용 → 체크리스트 ticket 충족 → v2 등록 →
v1 사용자 주문(카트→주문→무료 확정 / 호스트 승인)으로 재고 감소 → SOLD 상태 → 판매된 티켓 수정 제한·삭제 불가 →
판매 중단(v1 카트 400)·재개 → 옵션 잠금(추가금 변경·삭제·옵션 변경 불가) → 권한 경계 / IDOR → v1 목록 호환.

재실행해도 충돌하지 않도록 유저 이메일에 실행마다 다른 접미사를 붙인다. DB 직접 접근은 하지 않는다.
"""
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
SECTIONS = [{"title": "공연 소개", "content": "<p>티켓 테스트</p>", "sortOrder": 0}]
ACCOUNT = {"bank": "신한은행", "holder": "고스락", "number": "110-123-456789"}


class V2TicketState:
    tokens: dict = {}
    emails: dict = {}
    host_id: int = 0
    other_host_id: int = 0
    event_id: int = 0
    other_event_id: int = 0
    no_ticket_event_id: int = 0
    dudoong_id: int = 0
    free_id: int = 0
    unlimited_id: int = 0
    other_ticket_id: int = 0
    yes_no_id: int = 0
    subjective_id: int = 0
    other_option_id: int = 0


@pytest.fixture(scope="module")
def s():
    return V2TicketState()


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


def _code(resp):
    return resp.json().get("code")


def _dudoong(**overrides):
    body = {
        "payType": "DUDOONG", "name": "일반", "description": "일반 입장", "price": 6000, "supplyCount": 10,
        "account": ACCOUNT, "approvalRequired": True, "isQuantityPublic": True, "purchaseLimit": 4,
        "saleStartAt": None, "saleEndAt": None,
    }
    body.update(overrides)
    return body


def _free(**overrides):
    body = _dudoong(payType="FREE", name="무료", price=0, account=None, approvalRequired=False)
    body.update(overrides)
    return body


def _tickets(base_url, s, event_id=None, who="guest"):
    resp = requests.get(f"{base_url}/v2/events/{event_id or s.event_id}/ticket-items/manage", headers=_h(s, who))
    assert_status(resp, 200)
    return {t["ticketItemId"]: t for t in get_data(resp)}


def _options(base_url, s, who="guest"):
    resp = requests.get(f"{base_url}/v2/events/{s.event_id}/options", headers=_h(s, who))
    assert_status(resp, 200)
    return {o["optionId"]: o for o in get_data(resp)}


def _ticket_url(base_url, s, ticket_id, suffix=""):
    return f"{base_url}/v2/events/{s.event_id}/ticket-items/{ticket_id}{suffix}"


def _v1_answers(base_url, s, ticket_id):
    resp = requests.get(f"{base_url}/v1/events/{s.event_id}/ticketItems/{ticket_id}/options", headers=_h(s, "buyer"))
    assert_status(resp, 200)
    answers = []
    for group in get_data(resp)["optionGroups"]:
        rows = group["options"]
        if len(rows) == 1:
            answers.append({"optionId": rows[0]["optionId"], "answer": "홍길동"})
        else:
            answers.append({"optionId": next(r for r in rows if r["answer"] == "예")["optionId"], "answer": "예"})
    return answers


def _v1_cart(base_url, s, ticket_id, quantity=1):
    return requests.post(
        f"{base_url}/v1/carts",
        json={"items": [{"itemId": ticket_id, "quantity": quantity, "options": _v1_answers(base_url, s, ticket_id)}]},
        headers=_h(s, "buyer"),
    )


def _v1_order(base_url, s, ticket_id, quantity=1):
    cart = _v1_cart(base_url, s, ticket_id, quantity)
    assert cart.status_code in (200, 201), cart.text[:300]
    resp = requests.post(f"{base_url}/v1/orders/", json={"cartId": get_data(cart)["cartId"], "couponId": None}, headers=_h(s, "buyer"))
    assert resp.status_code in (200, 201), resp.text[:300]
    return get_data(resp)["orderId"]


def test_01_setup(base_url, s):
    for who, name in [("master", "티켓마스터"), ("manager", "티켓매니저"), ("guest", "티켓일반"), ("outsider", "티켓외부"), ("other", "다른호스트"), ("buyer", "티켓구매자")]:
        email = f"v2ticket-{who}-{RUN}@dudoong.com"
        s.tokens[who], s.emails[who] = _login(base_url, email, name), email

    for who, attr, host_name in [("master", "host_id", f"티켓{RUN[:5]}"), ("other", "other_host_id", f"남티켓{RUN[:4]}")]:
        resp = requests.post(f"{base_url}/v2/hosts", json={"name": host_name, "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, who))
        assert_status(resp, 200)
        setattr(s, attr, get_data(resp)["hostId"])
    resp = requests.post(
        f"{base_url}/v2/hosts/{s.host_id}/members",
        json={"members": [{"email": s.emails["manager"], "role": "MANAGER"}, {"email": s.emails["guest"], "role": "GUEST"}]},
        headers=_h(s, "master"),
    )
    assert_status(resp, 200)

    for who, host_attr, attr, has_ticket in [
        ("manager", "host_id", "event_id", True), ("manager", "host_id", "no_ticket_event_id", False), ("other", "other_host_id", "other_event_id", True),
    ]:
        resp = requests.post(
            f"{base_url}/v2/events",
            json={"hostId": getattr(s, host_attr), "name": "v2티켓공연", "startAt": _f(START), "endAt": _f(END), "hasTicket": has_ticket},
            headers=_h(s, who),
        )
        assert_status(resp, 200)
        setattr(s, attr, get_data(resp)["eventId"])


def test_02_create_validation(base_url, s):
    url = f"{base_url}/v2/events/{s.event_id}/ticket-items"
    resp = requests.post(f"{base_url}/v2/events/{s.no_ticket_event_id}/ticket-items", json=_dudoong(), headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert _code(resp) == "Ticket_Item_400_12"
    for body, code in [
        (_dudoong(payType="PRICE"), "Ticket_Item_400_11"),
        (_dudoong(account=None), "Ticket_Item_400_8"),
        (_dudoong(price=0), "Ticket_Item_400_3"),
        (_free(price=1000), "Ticket_Item_400_3"),
        (_dudoong(saleStartAt=_f(START - timedelta(days=1)), saleEndAt=_f(START - timedelta(days=2))), "Ticket_Item_400_13"),
        (_dudoong(saleEndAt=_f(START + timedelta(minutes=1))), "Ticket_Item_400_13"),
    ]:
        resp = requests.post(url, json=body, headers=_h(s, "manager"))
        assert_status(resp, 400)
        assert _code(resp) == code, resp.text[:300]
    assert_status(requests.post(url, json=_dudoong(name="가" * 13), headers=_h(s, "manager")), 400)
    # 권한
    assert_status(requests.post(url, json=_dudoong(), headers=_h(s, "guest")), 403)
    assert_status(requests.post(url, json=_dudoong(), headers=_h(s, "outsider")), 403)
    assert_status(requests.post(url, json=_dudoong()), 401)
    assert_status(requests.post(f"{base_url}/v2/events/{s.other_event_id}/ticket-items", json=_dudoong(), headers=_h(s, "manager")), 403)
    assert _tickets(base_url, s) == {}


def test_03_create_tickets(base_url, s):
    url = f"{base_url}/v2/events/{s.event_id}/ticket-items"
    resp = requests.post(url, json=_dudoong(approvalRequired=False), headers=_h(s, "manager"))
    assert_status(resp, 200)
    data = get_data(resp)
    s.dudoong_id = data["ticketItemId"]
    assert data["payType"] == "DUDOONG" and data["approvalRequired"] is True
    assert data["saleState"] == "BEFORE_SALE" and data["remaining"] == 10 and data["account"] == ACCOUNT

    resp = requests.post(url, json=_free(supplyCount=10), headers=_h(s, "manager"))
    assert_status(resp, 200)
    s.free_id = get_data(resp)["ticketItemId"]

    # 무제한 + 재고 공개는 400
    resp = requests.post(url, json=_free(name="무제한", supplyCount=None, isQuantityPublic=True), headers=_h(s, "master"))
    assert_status(resp, 400)
    assert _code(resp) == "Ticket_Item_400_15"
    resp = requests.post(url, json=_free(name="무제한", supplyCount=None, isQuantityPublic=False, purchaseLimit=None, saleEndAt=_f(START)), headers=_h(s, "master"))
    assert_status(resp, 200)
    data = get_data(resp)
    s.unlimited_id = data["ticketItemId"]
    assert data["supplyCount"] is None and data["remaining"] is None and data["purchaseLimit"] is None
    assert data["isQuantityPublic"] is False and data["saleEndAt"] == _f(START)

    resp = requests.post(f"{base_url}/v2/events/{s.other_event_id}/ticket-items", json=_free(), headers=_h(s, "other"))
    assert_status(resp, 200)
    s.other_ticket_id = get_data(resp)["ticketItemId"]

    tickets = _tickets(base_url, s)
    assert set(tickets) == {s.dudoong_id, s.free_id, s.unlimited_id}
    # 비멤버는 관리 목록 403
    assert_status(requests.get(f"{base_url}/v2/events/{s.event_id}/ticket-items/manage", headers=_h(s, "outsider")), 403)


def test_04_options(base_url, s):
    url = f"{base_url}/v2/events/{s.event_id}/options"
    resp = requests.post(url, json={"name": "뒷풀이", "description": "참석하나요?", "type": "YES_NO", "yesAdditionalPrice": 1000}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    s.yes_no_id = get_data(resp)["optionId"]
    resp = requests.post(url, json={"name": "입금자명", "description": "입금자명을 적어주세요", "type": "SUBJECTIVE"}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    s.subjective_id = get_data(resp)["optionId"]
    resp = requests.post(url, json={"name": "x", "description": "x", "type": "SUBJECTIVE", "yesAdditionalPrice": 100}, headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert_status(requests.post(url, json={"name": "x", "description": "x", "type": "YES_NO"}, headers=_h(s, "guest")), 403)
    resp = requests.post(f"{base_url}/v2/events/{s.other_event_id}/options", json={"name": "남옵션", "description": "x", "type": "YES_NO"}, headers=_h(s, "other"))
    assert_status(resp, 200)
    s.other_option_id = get_data(resp)["optionId"]

    resp = requests.put(_ticket_url(base_url, s, s.dudoong_id, "/options"), json={"optionIds": [s.subjective_id, s.yes_no_id]}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert [o["optionId"] for o in get_data(resp)["options"]] == sorted([s.yes_no_id, s.subjective_id])
    # 무료티켓에 유료 옵션 불가, 다른 공연 옵션 불가
    resp = requests.put(_ticket_url(base_url, s, s.free_id, "/options"), json={"optionIds": [s.yes_no_id]}, headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert _code(resp) == "Item_Option_Group_400_4"
    resp = requests.put(_ticket_url(base_url, s, s.free_id, "/options"), json={"optionIds": [s.other_option_id]}, headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert _code(resp) == "Option_Group_400_1"
    assert_status(requests.put(_ticket_url(base_url, s, s.free_id, "/options"), json={"optionIds": [s.subjective_id]}, headers=_h(s, "manager")), 200)

    options = _options(base_url, s)
    assert options[s.subjective_id]["appliedTicketItemIds"] == sorted([s.dudoong_id, s.free_id])
    assert options[s.yes_no_id]["yesAdditionalPrice"] == 1000 and options[s.yes_no_id]["isLocked"] is False


def test_05_checklist_and_open(base_url, s):
    resp = requests.get(f"{base_url}/v2/events/{s.event_id}/checklist", headers=_h(s, "guest"))
    assert_status(resp, 200)
    assert get_data(resp)["ticket"] is True
    key = get_data(requests.post(f"{base_url}/v2/events/{s.event_id}/images", json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "manager")))["key"]
    resp = requests.patch(
        f"{base_url}/v2/events/{s.event_id}/basic",
        json={"posterImageKey": key, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]},
        headers=_h(s, "manager"),
    )
    assert_status(resp, 200)
    assert_status(requests.put(f"{base_url}/v2/events/{s.event_id}/sections", json=SECTIONS, headers=_h(s, "manager")), 200)
    resp = requests.post(f"{base_url}/v2/events/{s.event_id}/open", headers=_h(s, "manager"))
    assert_status(resp, 200)
    tickets = _tickets(base_url, s)
    assert tickets[s.free_id]["isPurchasable"] is True


def test_06_v1_list_compat(base_url, s):
    resp = requests.get(f"{base_url}/v1/events/{s.event_id}/ticketItems")
    assert_status(resp, 200)
    items = {t["ticketItemId"]: t for t in get_data(resp)["ticketItems"]}
    assert items[s.dudoong_id]["payType"] == "두둥티켓" and items[s.dudoong_id]["approveType"] == "승인"
    assert items[s.free_id]["payType"] == "무료티켓" and items[s.free_id]["approveType"] == "선착순"
    assert items[s.dudoong_id]["supplyCount"] == 10
    # v1 프론트용 무제한 판정 필드 (숫자 필드는 하위 호환으로 유지)
    assert items[s.dudoong_id]["isUnlimitedSupply"] is False and items[s.dudoong_id]["hasNoPurchaseLimit"] is False
    assert items[s.unlimited_id]["isUnlimitedSupply"] is True and items[s.unlimited_id]["hasNoPurchaseLimit"] is True
    assert items[s.unlimited_id]["supplyCount"] >= 1_000_000


def test_07_v1_order_reduces_stock(base_url, s):
    # 무료(선착순): 카트 → 주문 → 무료 확정
    order = _v1_order(base_url, s, s.free_id)
    assert_status(requests.post(f"{base_url}/v1/orders/{order}/free", headers=_h(s, "buyer")), 200)
    # 두둥(승인): 카트 → 주문 → 호스트 승인
    order = _v1_order(base_url, s, s.dudoong_id)
    assert_status(requests.post(f"{base_url}/v1/events/{s.event_id}/orders/{order}/approve", headers=_h(s, "master")), 200)

    tickets = _tickets(base_url, s)
    for ticket_id in (s.free_id, s.dudoong_id):
        assert tickets[ticket_id]["saleState"] == "SOLD"
        assert tickets[ticket_id]["isSold"] is True
        assert tickets[ticket_id]["remaining"] == 9 and tickets[ticket_id]["soldCount"] == 1
    assert tickets[s.unlimited_id]["saleState"] == "BEFORE_SALE"


def test_07b_pending_approve_lock(base_url, s):
    """승인 대기 주문만 있어도(재고 감소 전) 잠김: 가격·계좌·옵션 추가금 변경, 옵션 삭제·떼기 400"""
    resp = requests.post(f"{base_url}/v2/events/{s.event_id}/options", json={"name": "굿즈", "description": "굿즈 구매?", "type": "YES_NO", "yesAdditionalPrice": 3000}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    option_id = get_data(resp)["optionId"]
    resp = requests.post(f"{base_url}/v2/events/{s.event_id}/ticket-items", json=_dudoong(name="승인대기"), headers=_h(s, "manager"))
    assert_status(resp, 200)
    ticket_id = get_data(resp)["ticketItemId"]
    assert_status(requests.put(_ticket_url(base_url, s, ticket_id, "/options"), json={"optionIds": [option_id]}, headers=_h(s, "manager")), 200)
    order = _v1_order(base_url, s, ticket_id)

    t = _tickets(base_url, s)[ticket_id]
    assert t["saleState"] == "BEFORE_SALE" and t["isSold"] is False and t["hasPendingOrders"] is True
    for body in [_dudoong(name="승인대기", price=7000), _dudoong(name="승인대기", account={**ACCOUNT, "number": "999"})]:
        resp = requests.patch(_ticket_url(base_url, s, ticket_id), json=body, headers=_h(s, "manager"))
        assert_status(resp, 400)
        assert _code(resp) == "Ticket_Item_400_14"
    opt_url = f"{base_url}/v2/events/{s.event_id}/options/{option_id}"
    resp = requests.patch(opt_url, json={"yesAdditionalPrice": 5000}, headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert _code(resp) == "Option_Group_400_5"
    resp = requests.delete(opt_url, headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert _code(resp) == "Option_Group_400_2"
    resp = requests.put(_ticket_url(base_url, s, ticket_id, "/options"), json={"optionIds": []}, headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert _code(resp) == "Item_Option_Group_400_2"
    assert _options(base_url, s)[option_id]["isLocked"] is True
    # 판매 중단 후에도 기존 주문 승인은 된다
    assert_status(requests.post(_ticket_url(base_url, s, ticket_id, "/suspend"), headers=_h(s, "manager")), 200)
    assert_status(requests.post(f"{base_url}/v1/events/{s.event_id}/orders/{order}/approve", headers=_h(s, "master")), 200)
    t = _tickets(base_url, s)[ticket_id]
    assert t["saleState"] == "SUSPENDED" and t["isSold"] is True and t["hasPendingOrders"] is False


def test_08_sold_ticket_restrictions(base_url, s):
    url = _ticket_url(base_url, s, s.dudoong_id)
    for body in [_dudoong(name="이름변경"), _dudoong(price=7000), _dudoong(supplyCount=9), _dudoong(account={**ACCOUNT, "number": "999"}), _free()]:
        resp = requests.patch(url, json=body, headers=_h(s, "manager"))
        assert_status(resp, 400)
        assert _code(resp) == "Ticket_Item_400_14", resp.text[:300]
    resp = requests.patch(url, json=_dudoong(description="설명변경", supplyCount=20, isQuantityPublic=False, purchaseLimit=2, saleEndAt=_f(START - timedelta(hours=1))), headers=_h(s, "manager"))
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["description"] == "설명변경" and data["supplyCount"] == 20 and data["remaining"] == 19 and data["soldCount"] == 1
    assert data["purchaseLimit"] == 2 and data["isQuantityPublic"] is False

    resp = requests.delete(url, headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert _code(resp) == "Ticket_Item_400_7"
    # 판매 전 티켓은 삭제 가능
    resp = requests.delete(_ticket_url(base_url, s, s.unlimited_id), headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert s.unlimited_id not in {t["ticketItemId"] for t in get_data(resp)}


def test_09_suspend_resume(base_url, s):
    assert_status(requests.post(_ticket_url(base_url, s, s.free_id, "/suspend"), headers=_h(s, "guest")), 403)
    for _ in range(2):  # 멱등
        resp = requests.post(_ticket_url(base_url, s, s.free_id, "/suspend"), headers=_h(s, "manager"))
        assert_status(resp, 200)
        assert get_data(resp)["saleState"] == "SUSPENDED" and get_data(resp)["isSold"] is True
    resp = _v1_cart(base_url, s, s.free_id)
    assert_status(resp, 400)
    assert _code(resp) == "Ticket_Item_400_10"
    # v1 공개 목록에서는 빠지고, v1 어드민 목록에는 보인다
    public_ids = {t["ticketItemId"] for t in get_data(requests.get(f"{base_url}/v1/events/{s.event_id}/ticketItems"))["ticketItems"]}
    assert s.free_id not in public_ids and s.dudoong_id in public_ids
    admin_ids = {t["ticketItemId"] for t in get_data(requests.get(f"{base_url}/v1/events/{s.event_id}/ticketItems/admin", headers=_h(s, "guest")))["ticketItems"]}
    assert s.free_id in admin_ids

    resp = requests.post(_ticket_url(base_url, s, s.free_id, "/resume"), headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert get_data(resp)["saleState"] == "SOLD"
    resp = _v1_cart(base_url, s, s.free_id)
    assert resp.status_code in (200, 201), resp.text[:300]
    public_ids = {t["ticketItemId"] for t in get_data(requests.get(f"{base_url}/v1/events/{s.event_id}/ticketItems"))["ticketItems"]}
    assert s.free_id in public_ids


def test_10_option_lock(base_url, s):
    options = _options(base_url, s)
    assert options[s.yes_no_id]["isLocked"] is True and options[s.subjective_id]["isLocked"] is True
    url = f"{base_url}/v2/events/{s.event_id}/options/{s.yes_no_id}"
    resp = requests.patch(url, json={"name": "뒷풀이 참석", "description": "새 설명"}, headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert get_data(resp)["name"] == "뒷풀이 참석"
    resp = requests.patch(url, json={"yesAdditionalPrice": 3000}, headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert _code(resp) == "Option_Group_400_5"
    resp = requests.delete(url, headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert _code(resp) == "Option_Group_400_2"
    resp = requests.put(_ticket_url(base_url, s, s.dudoong_id, "/options"), json={"optionIds": [s.yes_no_id]}, headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert _code(resp) == "Item_Option_Group_400_2"
    assert _options(base_url, s)[s.yes_no_id]["yesAdditionalPrice"] == 1000


def test_11_idor(base_url, s):
    # 다른 공연의 티켓·옵션 id 를 내 공연 경로로 → 404, 남의 공연 경로 → 403
    resp = requests.patch(_ticket_url(base_url, s, s.other_ticket_id), json=_free(), headers=_h(s, "manager"))
    assert_status(resp, 404)
    assert_status(requests.delete(_ticket_url(base_url, s, s.other_ticket_id), headers=_h(s, "manager")), 404)
    assert_status(requests.post(_ticket_url(base_url, s, s.other_ticket_id, "/suspend"), headers=_h(s, "manager")), 404)
    resp = requests.patch(f"{base_url}/v2/events/{s.event_id}/options/{s.other_option_id}", json={"name": "탈취"}, headers=_h(s, "manager"))
    assert_status(resp, 404)
    resp = requests.patch(
        f"{base_url}/v2/events/{s.other_event_id}/ticket-items/{s.other_ticket_id}", json=_free(name="탈취"), headers=_h(s, "manager"),
    )
    assert_status(resp, 403)
    resp = requests.get(f"{base_url}/v2/events/{s.other_event_id}/ticket-items/manage", headers=_h(s, "other"))
    assert_status(resp, 200)
    assert get_data(resp)[0]["name"] == "무료" and get_data(resp)[0]["saleState"] == "BEFORE_SALE"


def test_12_v1_create_on_no_ticket_event_unchanged(base_url, s):
    """v1 은 hasTicket 을 모르므로 티켓 없음 공연에도 v1 티켓 생성이 된다 (v1 동작 불변). v2 체크리스트는 면제 + 실제 상태를 함께 보여준다"""
    resp = requests.post(
        f"{base_url}/v1/events/{s.no_ticket_event_id}/ticketItems",
        json={"payType": "무료티켓", "name": "v1무료", "description": "v1", "price": 0, "supplyCount": 10,
              "approveType": "선착순", "isQuantityPublic": True, "purchaseLimit": 1},
        headers=_h(s, "master"),
    )
    assert resp.status_code in (200, 201), resp.text[:300]
    checklist = get_data(requests.get(f"{base_url}/v2/events/{s.no_ticket_event_id}/checklist", headers=_h(s, "guest")))
    assert checklist["ticketRequired"] is False and checklist["ticket"] is True
    tickets = _tickets(base_url, s, event_id=s.no_ticket_event_id)
    assert len(tickets) == 1 and list(tickets.values())[0]["payType"] == "FREE"


def test_13_v1_long_name_resend(base_url, s):
    """v1 로 만든 13자 이름(앞뒤 공백) 티켓이 판매된 뒤 v2 폼을 그대로 재전송해 설명만 바꿀 수 있다 (양쪽 trim 비교, 바뀐 값만 길이 검증)"""
    long_name = " 열세글자이름입니다아아아아 "
    resp = requests.post(
        f"{base_url}/v1/events/{s.event_id}/ticketItems",
        json={"payType": "무료티켓", "name": long_name, "description": "v1", "price": 0, "supplyCount": 10,
              "approveType": "선착순", "isQuantityPublic": True, "purchaseLimit": 2},
        headers=_h(s, "master"),
    )
    assert resp.status_code in (200, 201), resp.text[:300]
    ticket_id = get_data(resp)["ticketItemId"]
    order = _v1_order(base_url, s, ticket_id)
    assert_status(requests.post(f"{base_url}/v1/orders/{order}/free", headers=_h(s, "buyer")), 200)
    current = _tickets(base_url, s)[ticket_id]
    assert current["saleState"] == "SOLD" and current["name"] == long_name

    form = _free(name=current["name"], description="새 설명", supplyCount=10, purchaseLimit=2)
    resp = requests.patch(_ticket_url(base_url, s, ticket_id), json=form, headers=_h(s, "manager"))
    assert_status(resp, 200)
    assert get_data(resp)["name"] == long_name and get_data(resp)["description"] == "새 설명"
    resp = requests.patch(_ticket_url(base_url, s, ticket_id), json={**form, "name": "새이름"}, headers=_h(s, "manager"))
    assert_status(resp, 400)
    assert _code(resp) == "Ticket_Item_400_14"
