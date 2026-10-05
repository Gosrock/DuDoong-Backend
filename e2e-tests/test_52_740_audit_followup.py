"""
전수조사 후속 E2E (#740): 공연 장소 상세주소 / 호스트 발급 티켓 선물 상태·주문자/소유자 / dDay(E-3·D-1·H-14).

시나리오: 호스트·공연(무료 선착순 티켓) 준비 → E-4 상세주소 저장 → E-3·E-1·P-3·H-14 에 상세주소 →
보낸 사람이 무료 2장 주문 → 1장 선물 대기(G-1) → I-1 에 PENDING → 다른 1장 선물 수락(G-4) → I-1·I-2 에 ACCEPTED·주문자/소유자 분리 →
I-3 엑셀 소유자·선물 열 → E-3·D-1·H-14 dDay.

재실행해도 충돌하지 않도록 유저 이메일에 실행마다 다른 접미사를 붙인다. DB 직접 접근은 하지 않는다.
"""
import io
import re
import uuid
import zipfile
from datetime import datetime, timedelta
from xml.etree import ElementTree

import pytest
import requests

from conftest import assert_status, get_data

RUN = uuid.uuid4().hex[:8]
FMT = "%Y.%m.%d %H:%M"
START = (datetime.now() + timedelta(days=12)).replace(hour=19, minute=0, second=0, microsecond=0)
PLACE = {"name": "롤링홀", "address": "서울 마포구 어울마당로 35", "latitude": 37.548369, "longitude": 126.920036}
SECTIONS = [{"title": "공연 소개", "content": "<p>전수조사 후속</p>", "sortOrder": 0}]
PHONES = {"master": "010-1000-0001", "sender": "010-2000-0002", "receiver": "010-3000-0003", "receiver2": "010-4000-0004"}


class AuditState:
    tokens: dict = {}
    host_id: int = 0
    event_id: int = 0
    ticket_id: int = 0
    order_uuid: str = ""
    ticket_uuids: list = []
    ticket_nos: list = []
    accepted_uuid: str = ""


@pytest.fixture(scope="module")
def s():
    return AuditState()


def _h(s, who):
    return {"Authorization": f"Bearer {s.tokens[who]}"}


def _login(base_url, email, name, phone):
    resp = requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={"email": email, "name": name, "phoneNumber": phone, "profileImage": None, "marketingAgree": False},
    )
    assert_status(resp, 200)
    return get_data(resp)["accessToken"]


def _ev(base_url, s, path=""):
    return f"{base_url}/v2/events/{s.event_id}{path}"


def _issued(base_url, s):
    resp = requests.get(_ev(base_url, s, "/issued-tickets"), params={"size": 100}, headers=_h(s, "master"))
    assert_status(resp, 200)
    return {t["issuedTicketNo"]: t for t in get_data(resp)["tickets"]["content"]}


def _xlsx_rows(content):
    """첫 시트의 행 목록 (각 행은 셀 문자열 리스트). test_42 와 같은 파서"""
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
                value = "" if v is None else v.text
                cells.append(shared[int(value)] if c.get("t") == "s" and value != "" else value)
            rows.append(cells)
        return rows


def test_01_setup(base_url, s):
    for who, name in [("master", "후속마스터"), ("sender", "후속보낸이"), ("receiver", "후속받는이"), ("receiver2", "후속받는이2")]:
        s.tokens[who] = _login(base_url, f"v2audit-{who}-{RUN}@dudoong.com", name, PHONES[who])
    resp = requests.post(f"{base_url}/v2/hosts", json={"name": f"후속{RUN[:5]}", "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    s.host_id = get_data(resp)["hostId"]
    resp = requests.post(
        f"{base_url}/v2/events",
        json={"hostId": s.host_id, "name": "후속공연", "startAt": START.strftime(FMT), "endAt": (START + timedelta(hours=2)).strftime(FMT), "hasTicket": True},
        headers=_h(s, "master"),
    )
    assert_status(resp, 200)
    s.event_id = get_data(resp)["eventId"]
    resp = requests.post(_ev(base_url, s, "/ticket-items"), json={
        "payType": "FREE", "name": "무료", "description": "무료", "price": 0, "supplyCount": 10, "account": None,
        "approvalRequired": False, "isQuantityPublic": True, "purchaseLimit": 4, "saleStartAt": None, "saleEndAt": None,
    }, headers=_h(s, "master"))
    assert_status(resp, 200)
    s.ticket_id = get_data(resp)["ticketItemId"]


def test_02_place_detail_address(base_url, s):
    # 제어 문자·앞뒤 공백은 정리되어 저장된다
    resp = requests.patch(_ev(base_url, s, "/basic"), json={"place": {**PLACE, "detailAddress": "  지하 1층\n"}, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    assert get_data(resp)["place"]["detailAddress"] == "지하 1층"
    assert_status(requests.patch(_ev(base_url, s, "/basic"), json={"place": {**PLACE, "detailAddress": "가" * 256}}, headers=_h(s, "master")), 400)

    manage = get_data(requests.get(_ev(base_url, s, "/manage"), headers=_h(s, "master")))
    assert manage["place"]["detailAddress"] == "지하 1층"
    mine = get_data(requests.get(f"{base_url}/v2/me/events", params={"size": 50}, headers=_h(s, "master")))["content"]
    assert next(e for e in mine if e["eventId"] == s.event_id)["placeDetailAddress"] == "지하 1층"

    # 등록(OPEN) 후 공개 상세(P-3)·호스트 공연 리스트(H-14)
    key = get_data(requests.post(_ev(base_url, s, "/images"), json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "master")))["key"]
    assert_status(requests.patch(_ev(base_url, s, "/basic"), json={"posterImageKey": key}, headers=_h(s, "master")), 200)
    assert_status(requests.put(_ev(base_url, s, "/sections"), json=SECTIONS, headers=_h(s, "master")), 200)
    assert_status(requests.post(_ev(base_url, s, "/open"), headers=_h(s, "master")), 200)
    assert get_data(requests.get(_ev(base_url, s)))["place"]["detailAddress"] == "지하 1층"
    host_events = get_data(requests.get(f"{base_url}/v2/hosts/{s.host_id}/events"))["content"]
    assert next(e for e in host_events if e["eventId"] == s.event_id)["place"]["detailAddress"] == "지하 1층"


def test_03_issued_tickets_gift_pending(base_url, s):
    resp = requests.post(f"{base_url}/v2/orders", json={
        "eventId": s.event_id, "ticketItemId": s.ticket_id, "quantity": 2,
        "options": {"applyToAll": True, "answers": []}, "perTicketOptions": None,
        "paymentMethod": "FREE", "depositorName": None, "agreeRefundPolicy": True,
    }, headers=_h(s, "sender"))
    assert_status(resp, 200)
    data = get_data(resp)
    s.order_uuid = data["orderUuid"]
    s.ticket_uuids = [t["ticketUuid"] for t in data["issuedTickets"]]
    s.ticket_nos = [t["issuedTicketNo"] for t in data["issuedTickets"]]

    before = _issued(base_url, s)
    assert all(before[no]["giftState"] == "NONE" and before[no]["buyerName"] == before[no]["ownerName"] == "후속보낸이" for no in s.ticket_nos)

    # 1장 선물 대기 → I-1 에 PENDING (소유자는 아직 보낸 사람)
    resp = requests.post(f"{base_url}/v2/me/tickets/{s.ticket_uuids[0]}/gift", json={"memo": None}, headers=_h(s, "sender"))
    assert_status(resp, 200)
    rows = _issued(base_url, s)
    assert rows[s.ticket_nos[0]]["giftState"] == "PENDING" and rows[s.ticket_nos[0]]["ownerName"] == "후속보낸이"
    assert rows[s.ticket_nos[1]]["giftState"] == "NONE"


def test_04_issued_tickets_gift_accepted(base_url, s):
    # 다른 1장은 선물 → 수락: 소유자 = 받은 사람, 주문자 = 보낸 사람
    created = get_data(requests.post(f"{base_url}/v2/me/tickets/{s.ticket_uuids[1]}/gift", json={"memo": None}, headers=_h(s, "sender")))
    resp = requests.post(f"{base_url}/v2/gifts/{created['giftToken']}/accept", headers=_h(s, "receiver"))
    assert_status(resp, 200)
    s.accepted_uuid = get_data(resp)["ticketUuid"]

    row = _issued(base_url, s)[s.ticket_nos[1]]
    assert row["giftState"] == "ACCEPTED" and row["buyerName"] == "후속보낸이" and row["ownerName"] == "후속받는이"
    detail = get_data(requests.get(_ev(base_url, s, f"/issued-tickets/{s.accepted_uuid}"), headers=_h(s, "master")))
    assert detail["ticket"]["giftState"] == "ACCEPTED"
    assert detail["buyerPhone"] == PHONES["sender"] and detail["ownerPhone"] == PHONES["receiver"]


def test_05_issued_tickets_excel(base_url, s):
    resp = requests.get(_ev(base_url, s, "/issued-tickets/export"), headers=_h(s, "master"))
    assert_status(resp, 200)
    rows = _xlsx_rows(resp.content)
    headers = rows[0]
    assert headers[:12] == ["티켓번호", "티켓 종류", "티켓 이름", "주문자", "연락처", "소유자", "소유자 연락처", "선물", "주문번호", "발급일시", "입장", "체크인 시각"]
    by_no = {r[0]: dict(zip(headers, r + [""] * (len(headers) - len(r)))) for r in rows[1:]}
    pending, accepted = by_no[s.ticket_nos[0]], by_no[s.ticket_nos[1]]
    assert pending["선물"] == "선물 대기" and pending["소유자"] == "후속보낸이"
    assert accepted["선물"] == "선물 완료" and accepted["주문자"] == "후속보낸이" and accepted["소유자"] == "후속받는이"
    assert accepted["연락처"] == PHONES["sender"] and accepted["소유자 연락처"] == PHONES["receiver"]


def test_06_pending_ticket_scan_rejected(base_url, s):
    # 목록에 '선물 대기'로 보이는 티켓은 현장 스캔에서도 거부된다 (호스트가 미리 알 수 있게 된 것)
    resp = requests.post(_ev(base_url, s, "/check-ins"), json={"ticketUuid": s.ticket_uuids[0]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["result"] == "GIFT_PENDING"
    assert data["ticket"]["buyerName"] == data["ticket"]["ownerName"] == "후속보낸이"


def test_06b_check_in_buyer_and_owner(base_url, s):
    # 체크인 응답(Q-2)도 I-1 과 같은 의미: buyerName = 주문자, ownerName = 현재 소유자(받은 사람)
    resp = requests.post(_ev(base_url, s, "/check-ins"), json={"ticketUuid": s.accepted_uuid}, headers=_h(s, "master"))
    assert_status(resp, 200)
    data = get_data(resp)
    assert data["result"] == "ENTERED"
    assert data["ticket"]["buyerName"] == "후속보낸이" and data["ticket"]["ownerName"] == "후속받는이"


def test_07_d_day(base_url, s):
    expected = (START.date() - datetime.now().date()).days
    manage = get_data(requests.get(_ev(base_url, s, "/manage"), headers=_h(s, "master")))
    dashboard = get_data(requests.get(_ev(base_url, s, "/dashboard"), headers=_h(s, "master")))
    host_event = next(e for e in get_data(requests.get(f"{base_url}/v2/hosts/{s.host_id}/events"))["content"] if e["eventId"] == s.event_id)
    mine = next(e for e in get_data(requests.get(f"{base_url}/v2/me/events", params={"size": 50}, headers=_h(s, "master")))["content"] if e["eventId"] == s.event_id)
    assert manage["dDay"] == dashboard["dDay"] == host_event["dDay"] == mine["dDay"] == expected
    assert dashboard["displayStatus"] == "UPCOMING"


def test_08_order_approval_required_and_payment_column(base_url, s):
    # 무료 선착순 주문은 주문 시점 승인형이 아니다
    detail = get_data(requests.get(f"{base_url}/v2/me/orders/{s.order_uuid}", headers=_h(s, "sender")))
    assert detail["approvalRequired"] is False
    # 주문 엑셀: 입금자명 다음에 결제 방식
    rows = _xlsx_rows(requests.get(_ev(base_url, s, "/orders/export"), headers=_h(s, "master")).content)
    headers = rows[0]
    assert headers.index("결제 방식") == headers.index("입금자명") + 1
    row = dict(zip(headers, next(r for r in rows[1:] if r[0] == detail["orderNo"])))
    assert row["결제 방식"] == "무료"
