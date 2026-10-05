"""
#730 v2 주문 엑셀(R-6) 옵션 응답 컬럼 E2E (MySQL, xlsx 파싱).

시나리오: 두둥티켓(옵션 2개: 네/아니오 '뒷풀이', 주관식 '메모') + 옵션 없는 티켓 → v2 등록 →
일괄 옵션 2장 / 티켓별 옵션 3장(수식으로 시작하는 응답 포함) / 옵션 없는 티켓 주문 → 1건 승인(발급) →
R-6 옵션 열: 한 주문 한 행, 여러 라인은 `응답 ×수량`(같은 응답 합산)을 줄바꿈으로, 빈 칸, 수식 방어 / I-3 과 같은 헤더 규칙.

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
START = (datetime.now() + timedelta(days=30)).replace(hour=18, minute=0, second=0, microsecond=0)
END = START + timedelta(minutes=120)
PLACE = {"name": "롤링홀", "address": "서울 마포구 어울마당로 35", "latitude": 37.548369, "longitude": 126.920036}
ACCOUNT = {"bank": "신한은행", "holder": "고스락", "number": "110-123-456789"}
ORDER_BASE = ["주문번호", "주문자", "연락처", "입금자명", "결제 방식", "티켓", "매수", "결제금액", "주문일시", "상태", "환불", "거절·취소 사유"]
TICKET_BASE = ["티켓번호", "티켓 종류", "티켓 이름", "주문자", "연락처", "소유자", "소유자 연락처", "선물", "주문번호", "발급일시", "입장", "체크인 시각"]


class ExcelState:
    tokens: dict = {}
    event_id: int = 0
    ticket: int = 0
    plain: int = 0
    yes_no: int = 0
    memo: int = 0
    orders: dict = {}


@pytest.fixture(scope="module")
def s():
    return ExcelState()


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


def _answers(s, yes, memo):
    return [{"optionId": s.yes_no, "answer": "YES" if yes else "NO"}, {"optionId": s.memo, "answer": memo}]


def _order(base_url, s, who, item_id, quantity, answers=None, per_ticket=None):
    body = {
        "eventId": s.event_id, "ticketItemId": item_id, "quantity": quantity,
        "options": {"applyToAll": per_ticket is None, "answers": answers or []}, "perTicketOptions": per_ticket,
        "paymentMethod": "BANK_TRANSFER", "depositorName": f"입금{who}", "agreeRefundPolicy": True,
    }
    resp = requests.post(f"{base_url}/v2/orders", json=body, headers=_h(s, who))
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


def _sheet(base_url, s, path):
    resp = requests.get(_ev(base_url, s, path), headers=_h(s, "guest"))
    assert_status(resp, 200)
    return _xlsx_rows(resp.content)


def test_01_setup(base_url, s):
    for who in ["master", "guest", "all", "per", "plain", "formula"]:
        s.tokens[who] = _login(base_url, f"fix730-{who}-{RUN}@dudoong.com", f"엑셀{who}")
    resp = requests.post(f"{base_url}/v2/hosts", json={"name": f"엑셀{RUN[:5]}", "contacts": [{"type": "EMAIL", "value": "h@dudoong.com"}]}, headers=_h(s, "master"))
    assert_status(resp, 200)
    host_id = get_data(resp)["hostId"]
    assert_status(requests.post(f"{base_url}/v2/hosts/{host_id}/members", json={"members": [{"email": f"fix730-guest-{RUN}@dudoong.com", "role": "GUEST"}]}, headers=_h(s, "master")), 200)
    resp = requests.post(
        f"{base_url}/v2/events",
        json={"hostId": host_id, "name": "730엑셀공연", "startAt": START.strftime(FMT), "endAt": END.strftime(FMT), "hasTicket": True},
        headers=_h(s, "master"),
    )
    assert_status(resp, 200)
    s.event_id = get_data(resp)["eventId"]
    for attr, name in [("ticket", "옵션석"), ("plain", "옵션없음")]:
        body = {"payType": "DUDOONG", "name": name, "description": "730", "price": 5000, "supplyCount": 20, "account": ACCOUNT,
                "approvalRequired": True, "isQuantityPublic": True, "purchaseLimit": 4, "saleStartAt": None, "saleEndAt": None}
        resp = requests.post(_ev(base_url, s, "/ticket-items"), json=body, headers=_h(s, "master"))
        assert_status(resp, 200)
        setattr(s, attr, get_data(resp)["ticketItemId"])
    for attr, body in [("yes_no", {"name": "뒷풀이", "description": "참석?", "type": "YES_NO", "yesAdditionalPrice": 1000}),
                       ("memo", {"name": "메모", "description": "메모", "type": "SUBJECTIVE"})]:
        resp = requests.post(_ev(base_url, s, "/options"), json=body, headers=_h(s, "master"))
        assert_status(resp, 200)
        setattr(s, attr, get_data(resp)["optionId"])
    assert_status(requests.put(_ev(base_url, s, f"/ticket-items/{s.ticket}/options"), json={"optionIds": [s.yes_no, s.memo]}, headers=_h(s, "master")), 200)
    key = get_data(requests.post(_ev(base_url, s, "/images"), json={"purpose": "POSTER", "extension": "PNG"}, headers=_h(s, "master")))["key"]
    assert_status(requests.patch(_ev(base_url, s, "/basic"), json={"posterImageKey": key, "place": PLACE, "contacts": [{"type": "EMAIL", "value": "a@a.com"}]}, headers=_h(s, "master")), 200)
    assert_status(requests.put(_ev(base_url, s, "/sections"), json=[{"title": "소개", "content": "<p>730</p>", "sortOrder": 0}], headers=_h(s, "master")), 200)
    assert_status(requests.post(_ev(base_url, s, "/open"), headers=_h(s, "master")), 200)


def test_02_orders(base_url, s):
    s.orders["all"] = _order(base_url, s, "all", s.ticket, 2, answers=_answers(s, True, "일괄"))
    per_ticket = [_answers(s, True, "=1+1"), _answers(s, False, "=1+1"), _answers(s, True, "김, 이")]
    s.orders["per"] = _order(base_url, s, "per", s.ticket, 3, per_ticket=per_ticket)
    s.orders["plain"] = _order(base_url, s, "plain", s.plain, 1)
    s.orders["formula"] = _order(base_url, s, "formula", s.ticket, 1, answers=_answers(s, False, "@SUM(1)\n둘째줄"))
    # 1건 승인 → 발급 티켓(I-3)에도 옵션 응답이 생긴다 (MySQL 은 3장 승인도 성공)
    assert_status(requests.post(_ev(base_url, s, f"/orders/{s.orders['per']['orderUuid']}/approve"), headers=_h(s, "master")), 200)


def test_03_order_excel_option_columns(base_url, s):
    rows = _sheet(base_url, s, "/orders/export")
    assert rows[0] == ORDER_BASE + ["뒷풀이", "메모"]
    assert len(rows) - 1 == 4, "주문 4건 = 4행 (라인 수와 무관)"
    by_no = {r[0]: r + [""] * (len(rows[0]) - len(r)) for r in rows[1:]}
    assert by_no[s.orders["all"]["orderNo"]][len(ORDER_BASE):] == ["예", "일괄"]
    # 응답 안의 쉼표와 헷갈리지 않게 응답끼리는 줄바꿈
    assert by_no[s.orders["per"]["orderNo"]][len(ORDER_BASE):] == ["예 ×2\n아니요 ×1", "'=1+1 ×2\n김, 이 ×1"]
    # 라인 1개도 수식 방어, 응답 안의 줄바꿈은 공백
    assert by_no[s.orders["formula"]["orderNo"]][len(ORDER_BASE):] == ["아니요", "'@SUM(1) 둘째줄"]
    assert by_no[s.orders["plain"]["orderNo"]][len(ORDER_BASE):] == ["", ""]


def test_04_same_header_rule_as_issued_ticket_excel(base_url, s):
    orders = _sheet(base_url, s, "/orders/export")
    tickets = _sheet(base_url, s, "/issued-tickets/export")
    assert tickets[0][:len(TICKET_BASE)] == TICKET_BASE
    assert orders[0][len(ORDER_BASE):] == tickets[0][len(TICKET_BASE):] == ["뒷풀이", "메모"]
    # 같은 규칙이지 같은 열 집합은 아니다: 필터로 옵션 답변이 있는 주문이 빠지면 옵션 열도 없다 (답변에 나온 옵션만)
    only_plain = _xlsx_rows(requests.get(_ev(base_url, s, "/orders/export"), params={"searchType": "DEPOSITOR_NAME", "keyword": "입금plain"}, headers=_h(s, "guest")).content)
    assert only_plain[0] == ORDER_BASE and len(only_plain) == 2
