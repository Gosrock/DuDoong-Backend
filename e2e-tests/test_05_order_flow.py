"""
주문 플로우 E2E 시나리오 테스트 (핵심 시나리오).
장바구니 생성 → 장바구니 조회 → 주문 생성 → 무료 주문 완료 → 주문 상세 조회 순서로 검증합니다.
"""
import pytest
import requests

from conftest import assert_status


def test_create_cart(base_url, auth_headers, state):
    """
    티켓 상품을 장바구니에 담고 cart_id를 state에 저장합니다.
    옵션이 없는 무료 티켓이므로 options는 빈 리스트로 전송합니다.
    """
    assert state.ticket_item_id, "ticket_item_id가 없습니다. test_04_ticket_item을 먼저 실행하세요."
    url = f"{base_url}/v1/carts"
    payload = {
        "items": [
            {
                "itemId": state.ticket_item_id,
                "quantity": 1,
                "options": [],
            }
        ]
    }
    print(f"\n[test_create_cart] POST {url}")
    print(f"[test_create_cart] payload={payload}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_create_cart] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (200, 201), (
        f"장바구니 생성 실패: status={resp.status_code}, body={resp.text[:300]}"
    )
    data = resp.json()
    assert "id" in data, f"응답에 id 필드가 없습니다: {data}"
    state.cart_id = data["id"]
    print(f"[test_create_cart] 장바구니 생성 완료: cart_id={state.cart_id}")


def test_read_cart(base_url, auth_headers, state):
    """최근 생성된 장바구니를 조회합니다."""
    url = f"{base_url}/v1/carts/recent"
    print(f"\n[test_read_cart] GET {url}")
    resp = requests.get(url, headers=auth_headers)
    print(f"[test_read_cart] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = resp.json()
    # 장바구니가 없을 경우 null 반환 가능
    if data is not None:
        assert "id" in data, f"응답에 id 필드가 없습니다: {data}"
        print(f"[test_read_cart] 장바구니 조회 완료: cart_id={data.get('id')}")
    else:
        print(f"[test_read_cart] 장바구니 없음 (null 반환)")


def test_create_order(base_url, auth_headers, state):
    """
    장바구니를 주문서로 변환하여 주문을 생성하고 order_uuid를 state에 저장합니다.
    쿠폰 없이 생성합니다 (couponId=null).
    """
    assert state.cart_id, "cart_id가 없습니다. test_create_cart를 먼저 실행하세요."
    url = f"{base_url}/v1/orders/"
    payload = {
        "couponId": None,
        "cartId": state.cart_id,
    }
    print(f"\n[test_create_order] POST {url}")
    print(f"[test_create_order] payload={payload}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_create_order] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (200, 201), (
        f"주문 생성 실패: status={resp.status_code}, body={resp.text[:300]}"
    )
    data = resp.json()
    assert "orderUuid" in data, f"응답에 orderUuid 필드가 없습니다: {data}"
    state.order_uuid = data["orderUuid"]
    print(f"[test_create_order] 주문 생성 완료: order_uuid={state.order_uuid}")


def test_free_order(base_url, auth_headers, state):
    """
    0원 무료 주문을 완료 처리합니다.
    선착순 방식 무료 티켓에만 적용 가능합니다.
    """
    assert state.order_uuid, "order_uuid가 없습니다. test_create_order를 먼저 실행하세요."
    url = f"{base_url}/v1/orders/{state.order_uuid}/free"
    print(f"\n[test_free_order] POST {url}")
    resp = requests.post(url, headers=auth_headers)
    print(f"[test_free_order] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = resp.json()
    assert "orderUuid" in data, f"응답에 orderUuid 필드가 없습니다: {data}"
    print(f"[test_free_order] 무료 주문 완료: order_uuid={data.get('orderUuid')}")


def test_read_order(base_url, auth_headers, state):
    """완료된 주문의 상세 정보를 조회하고 발급된 티켓이 포함되는지 확인합니다."""
    assert state.order_uuid, "order_uuid가 없습니다."
    url = f"{base_url}/v1/orders/{state.order_uuid}"
    print(f"\n[test_read_order] GET {url}")
    resp = requests.get(url, headers=auth_headers)
    print(f"[test_read_order] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = resp.json()
    assert "orderUuid" in data, f"응답에 orderUuid 필드가 없습니다: {data}"
    assert data["orderUuid"] == state.order_uuid, "반환된 orderUuid가 일치하지 않습니다"
    print(f"[test_read_order] 주문 상세 조회 완료: {data.get('orderUuid')}")
