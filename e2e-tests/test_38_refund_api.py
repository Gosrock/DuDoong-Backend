"""
환불 전용 API E2E 테스트.
호스트용 환불 조회/확인 API와 어드민용 환불 API를 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


def _create_and_refund_order(base_url, auth_headers, state) -> str:
    """
    환불 API 테스트 전용: 주문 생성 -> 무료 결제 -> 환불 요청 후 order_uuid 반환.
    """
    assert state.ticket_item_id, "ticket_item_id가 없습니다. test_04를 먼저 실행하세요."

    # 1) 장바구니 생성
    cart_resp = requests.post(
        f"{base_url}/v1/carts",
        json={"items": [{"itemId": state.ticket_item_id, "quantity": 1, "options": []}]},
        headers=auth_headers,
    )
    assert cart_resp.status_code in (200, 201), f"장바구니 생성 실패: {cart_resp.text[:200]}"
    cart_id = get_data(cart_resp)["cartId"]

    # 2) 주문 생성
    order_resp = requests.post(
        f"{base_url}/v1/orders/",
        json={"couponId": None, "cartId": cart_id},
        headers=auth_headers,
    )
    assert order_resp.status_code in (200, 201), f"주문 생성 실패: {order_resp.text[:200]}"
    order_uuid = get_data(order_resp)["orderId"]

    # 3) 무료 결제 완료
    free_resp = requests.post(
        f"{base_url}/v1/orders/{order_uuid}/free",
        headers=auth_headers,
    )
    assert free_resp.status_code == 200, f"무료 결제 실패: {free_resp.text[:200]}"

    # 4) 환불 요청
    refund_resp = requests.post(
        f"{base_url}/v1/orders/{order_uuid}/refund",
        headers=auth_headers,
    )
    assert refund_resp.status_code == 200, f"환불 요청 실패: {refund_resp.text[:200]}"

    return order_uuid


# ==================== 호스트용 환불 API ====================


class TestHostRefundApi:
    """호스트용 환불 API (/api/v1/events/{eventId}/refunds) 테스트"""

    @pytest.fixture(autouse=True)
    def setup(self, base_url, auth_headers, state):
        self.base_url = base_url
        self.auth_headers = auth_headers
        self.state = state

    def test_refund_list(self):
        """환불 목록 조회 API가 정상 응답한다."""
        assert self.state.event_id, "event_id가 없습니다."

        # 환불 대상 주문 생성
        order_uuid = _create_and_refund_order(
            self.base_url, self.auth_headers, self.state
        )
        self.state.refund_api_order_uuid = order_uuid

        url = f"{self.base_url}/v1/events/{self.state.event_id}/refunds"
        print(f"\n[test_refund_list] GET {url}")
        resp = requests.get(url, headers=self.auth_headers)
        print(f"[test_refund_list] status={resp.status_code}, body={resp.text[:400]}")

        assert_status(resp, 200)
        data = get_data(resp)
        assert "content" in data, f"응답에 content 필드 없음: {data}"
        assert len(data["content"]) > 0, "환불 목록이 비어있습니다."

    def test_refund_list_with_status_filter(self):
        """환불 목록 조회 시 refundStatus 필터가 동작한다."""
        url = f"{self.base_url}/v1/events/{self.state.event_id}/refunds"
        resp = requests.get(
            url,
            params={"refundStatus": "REFUND_REQUESTED"},
            headers=self.auth_headers,
        )
        assert_status(resp, 200)
        data = get_data(resp)
        for item in data.get("content", []):
            assert item["refundStatus"] == "REFUND_REQUESTED"

    def test_refund_detail(self):
        """환불 상세 조회 API가 정상 응답한다."""
        order_uuid = getattr(self.state, "refund_api_order_uuid", None)
        if not order_uuid:
            pytest.skip("환불 주문이 없습니다.")

        url = f"{self.base_url}/v1/events/{self.state.event_id}/refunds/{order_uuid}"
        print(f"\n[test_refund_detail] GET {url}")
        resp = requests.get(url, headers=self.auth_headers)
        print(f"[test_refund_detail] status={resp.status_code}, body={resp.text[:400]}")

        assert_status(resp, 200)
        data = get_data(resp)
        assert data["orderId"] == order_uuid
        assert data["refundStatus"] == "REFUND_REQUESTED"

    def test_complete_refund(self):
        """환불 확인 API가 정상 동작한다."""
        order_uuid = getattr(self.state, "refund_api_order_uuid", None)
        if not order_uuid:
            pytest.skip("환불 주문이 없습니다.")

        url = f"{self.base_url}/v1/events/{self.state.event_id}/refunds/{order_uuid}/complete"
        print(f"\n[test_complete_refund] PATCH {url}")
        resp = requests.patch(url, headers=self.auth_headers)
        print(f"[test_complete_refund] status={resp.status_code}, body={resp.text[:400]}")

        assert_status(resp, 200)
        data = get_data(resp)
        assert data["orderId"] == order_uuid
        assert data["refundStatus"] == "REFUND_COMPLETED"


# ==================== 어드민용 환불 API ====================


class TestAdminRefundApi:
    """어드민용 환불 API (/internal-api/v1/refunds) 테스트"""

    @pytest.fixture(autouse=True)
    def setup(self, base_url, auth_headers, state):
        self.base_url = base_url
        self.auth_headers = auth_headers
        self.state = state
        # internal-api base url
        self.admin_base = self.base_url.replace("/api", "")

    def test_admin_refund_list(self):
        """어드민 환불 목록 조회가 정상 응답한다."""
        url = f"{self.admin_base}/internal-api/v1/refunds"
        print(f"\n[test_admin_refund_list] GET {url}")
        resp = requests.get(url, headers=self.auth_headers)
        print(f"[test_admin_refund_list] status={resp.status_code}, body={resp.text[:400]}")

        # ADMIN 권한이 없으면 403이 정상
        if resp.status_code == 403:
            pytest.skip("ADMIN 권한이 없어 스킵합니다.")

        assert_status(resp, 200)
        data = get_data(resp)
        assert "content" in data, f"응답에 content 필드 없음: {data}"

    def test_admin_refund_detail(self):
        """어드민 환불 상세 조회가 정상 응답한다."""
        order_uuid = getattr(self.state, "refund_api_order_uuid", None)
        if not order_uuid:
            pytest.skip("환불 주문이 없습니다.")

        url = f"{self.admin_base}/internal-api/v1/refunds/{order_uuid}"
        resp = requests.get(url, headers=self.auth_headers)

        if resp.status_code == 403:
            pytest.skip("ADMIN 권한이 없어 스킵합니다.")

        assert_status(resp, 200)
        data = get_data(resp)
        assert data["orderId"] == order_uuid
        assert "userId" in data, "어드민 응답에 userId 필드가 없습니다."

    def test_admin_complete_refund(self):
        """어드민 환불 확인 API가 정상 동작한다."""
        # 새로운 환불 대상 주문 생성
        order_uuid = _create_and_refund_order(
            self.base_url, self.auth_headers, self.state
        )

        url = f"{self.admin_base}/internal-api/v1/refunds/{order_uuid}/complete"
        print(f"\n[test_admin_complete_refund] PATCH {url}")
        resp = requests.patch(url, headers=self.auth_headers)
        print(f"[test_admin_complete_refund] status={resp.status_code}, body={resp.text[:400]}")

        if resp.status_code == 403:
            pytest.skip("ADMIN 권한이 없어 스킵합니다.")

        assert_status(resp, 200)
        data = get_data(resp)
        assert data["orderId"] == order_uuid
        assert data["refundStatus"] == "REFUND_COMPLETED"
        assert "userId" in data
