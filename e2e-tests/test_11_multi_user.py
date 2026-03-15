"""
멀티 유저 시나리오 E2E 테스트.
두 번째 사용자가 첫 번째 사용자의 이벤트에서 주문하거나,
첫 번째 사용자의 이벤트를 수정하려 할 때의 권한 검증을 포함합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


# 이 테스트 모듈 내에서만 사용하는 로컬 상태
_user2_state: dict = {
    "access_token": "",
    "auth_headers": {},
    "cart_id": 0,
    "order_uuid": "",
}


def _login_user2(base_url: str) -> str:
    """두 번째 테스트 유저로 로그인하고 accessToken을 반환합니다."""
    url = f"{base_url}/v1/auth/oauth/local/login"
    payload = {
        "email": "user2@dudoong.com",
        "name": "E2E테스터2",
        "phoneNumber": "010-1111-2222",
        "profileImage": None,
        "marketingAgree": False,
    }
    print(f"[user2 login] POST {url}")
    resp = requests.post(url, json=payload)
    print(f"[user2 login] status={resp.status_code}, body={resp.text[:300]}")
    assert resp.status_code == 200, f"user2 로그인 실패: {resp.text}"
    data = get_data(resp)
    return data["accessToken"]


def test_create_second_user(base_url):
    """두 번째 사용자(user2@dudoong.com)로 로그인하고 토큰을 획득합니다."""
    print(f"\n[test_create_second_user] user2 로그인 시작")
    token = _login_user2(base_url)
    assert token, "user2 accessToken이 비어 있습니다"
    _user2_state["access_token"] = token
    _user2_state["auth_headers"] = {"Authorization": f"Bearer {token}"}
    print(f"[test_create_second_user] user2 토큰 획득 완료: {token[:30]}...")


def test_user2_orders_user1_event(base_url, state):
    """user2가 user1의 이벤트에서 장바구니를 만들고 주문을 생성합니다."""
    if not _user2_state["access_token"]:
        pytest.skip("user2 토큰이 없어 테스트를 건너뜁니다.")
    if not state.ticket_item_id:
        pytest.skip("ticket_item_id가 없어 테스트를 건너뜁니다.")

    headers2 = _user2_state["auth_headers"]

    # 장바구니 생성
    cart_url = f"{base_url}/v1/carts"
    cart_payload = {
        "items": [
            {
                "itemId": state.ticket_item_id,
                "quantity": 1,
                "options": [],
            }
        ]
    }
    print(f"\n[test_user2_orders_user1_event] POST {cart_url} (user2)")
    print(f"[test_user2_orders_user1_event] payload={cart_payload}")
    cart_resp = requests.post(cart_url, json=cart_payload, headers=headers2)
    print(f"[test_user2_orders_user1_event] cart status={cart_resp.status_code}, body={cart_resp.text[:400]}")

    assert cart_resp.status_code in (200, 201), (
        f"user2 장바구니 생성 실패: {cart_resp.text[:300]}"
    )
    cart_data = get_data(cart_resp)
    assert "cartId" in cart_data, f"응답에 cartId가 없습니다: {cart_data}"
    _user2_state["cart_id"] = cart_data["cartId"]
    print(f"[test_user2_orders_user1_event] user2 장바구니 생성: cart_id={_user2_state['cart_id']}")

    # 주문 생성
    order_url = f"{base_url}/v1/orders/"
    order_payload = {"couponId": None, "cartId": _user2_state["cart_id"]}
    print(f"[test_user2_orders_user1_event] POST {order_url} (user2)")
    order_resp = requests.post(order_url, json=order_payload, headers=headers2)
    print(f"[test_user2_orders_user1_event] order status={order_resp.status_code}, body={order_resp.text[:400]}")

    assert order_resp.status_code in (200, 201), (
        f"user2 주문 생성 실패: {order_resp.text[:300]}"
    )
    order_data = get_data(order_resp)
    assert "orderId" in order_data, f"응답에 orderId가 없습니다: {order_data}"
    _user2_state["order_uuid"] = order_data["orderId"]
    print(f"[test_user2_orders_user1_event] user2 주문 생성 완료: order_uuid={_user2_state['order_uuid']}")

    # 무료 결제 완료
    free_url = f"{base_url}/v1/orders/{_user2_state['order_uuid']}/free"
    print(f"[test_user2_orders_user1_event] POST {free_url} (user2)")
    free_resp = requests.post(free_url, headers=headers2)
    print(f"[test_user2_orders_user1_event] free status={free_resp.status_code}, body={free_resp.text[:400]}")
    assert_status(free_resp, 200)
    print(f"[test_user2_orders_user1_event] user2 무료 주문 완료")


def test_user2_cannot_modify_user1_event(base_url, state):
    """user2가 user1의 이벤트 기본 정보를 수정하려 하면 권한 오류가 발생해야 합니다."""
    if not _user2_state["access_token"]:
        pytest.skip("user2 토큰이 없어 테스트를 건너뜁니다.")
    if not state.event_id:
        pytest.skip("event_id가 없어 테스트를 건너뜁니다.")

    headers2 = _user2_state["auth_headers"]
    url = f"{base_url}/v1/events/{state.event_id}/basic"
    payload = {
        "name": "해킹시도공연",
        "startAt": "2099.01.01 12:00",
        "runTime": 999,
    }
    print(f"\n[test_user2_cannot_modify_user1_event] PATCH {url} (user2 - 권한 없음)")
    print(f"[test_user2_cannot_modify_user1_event] payload={payload}")
    resp = requests.patch(url, json=payload, headers=headers2)
    print(f"[test_user2_cannot_modify_user1_event] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (400, 401, 403, 404), (
        f"타 유저 이벤트 수정 시 권한 오류(400/401/403/404)가 기대되지만 {resp.status_code}가 반환되었습니다"
    )
    print(f"[test_user2_cannot_modify_user1_event] 타 유저 이벤트 수정 차단 확인: {resp.status_code}")
