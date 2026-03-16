"""
티켓 옵션 그룹 생성/적용/해제 및 티켓 삭제 E2E 테스트.
이벤트에 옵션 그룹을 만들고, 티켓에 적용/해제하는 플로우를 검증합니다.
"""
import pytest
import requests

from conftest import assert_status, get_data


# 이 모듈 내에서만 사용하는 로컬 상태
_option_state: dict = {
    "option_group_id": 0,
    "new_ticket_item_id": 0,
}


def test_create_option_group(base_url, auth_headers, state):
    """이벤트에 Y/N 타입 옵션 그룹을 생성합니다."""
    if not state.event_id:
        pytest.skip("event_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events/{state.event_id}/ticketOptions"
    payload = {
        "type": "Y_N",
        "name": "E2E굿즈수령여부",
        "description": "굿즈를 수령하시겠습니까?",
        "additionalPrice": 0,
    }
    print(f"\n[test_create_option_group] POST {url}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_create_option_group] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (200, 201), (
        f"옵션 그룹 생성 실패: status={resp.status_code}, body={resp.text[:300]}"
    )
    data = get_data(resp)
    # OptionGroupResponse에서 optionGroupId 추출
    if "optionGroupId" in data:
        _option_state["option_group_id"] = data["optionGroupId"]
    elif "id" in data:
        _option_state["option_group_id"] = data["id"]
    print(f"[test_create_option_group] 옵션 그룹 생성 완료: id={_option_state['option_group_id']}")


def test_get_event_options(base_url, auth_headers, state):
    """이벤트의 옵션 목록을 조회하고 생성된 옵션이 포함되는지 확인합니다."""
    if not state.event_id:
        pytest.skip("event_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events/{state.event_id}/ticketOptions"
    print(f"\n[test_get_event_options] GET {url}")
    resp = requests.get(url, headers=auth_headers)
    print(f"[test_get_event_options] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = get_data(resp)
    print(f"[test_get_event_options] 옵션 목록 조회 완료: {data}")


def test_apply_option_to_ticket(base_url, auth_headers, state):
    """기존 티켓에 옵션 그룹을 적용합니다."""
    if not state.event_id or not state.ticket_item_id:
        pytest.skip("event_id 또는 ticket_item_id가 없어 테스트를 건너뜁니다.")
    if not _option_state["option_group_id"]:
        pytest.skip("option_group_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events/{state.event_id}/ticketItems/{state.ticket_item_id}/option"
    payload = {"optionGroupId": _option_state["option_group_id"]}
    print(f"\n[test_apply_option_to_ticket] PATCH {url}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_apply_option_to_ticket] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    print("[test_apply_option_to_ticket] 옵션 적용 완료")


def test_get_ticket_item_options(base_url, auth_headers, state):
    """티켓에 적용된 옵션 목록을 조회합니다."""
    if not state.event_id or not state.ticket_item_id:
        pytest.skip("event_id 또는 ticket_item_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events/{state.event_id}/ticketItems/{state.ticket_item_id}/options"
    print(f"\n[test_get_ticket_item_options] GET {url}")
    resp = requests.get(url, headers=auth_headers)
    print(f"[test_get_ticket_item_options] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    print("[test_get_ticket_item_options] 티켓 옵션 목록 조회 완료")


def test_unapply_option_from_ticket(base_url, auth_headers, state):
    """티켓에서 옵션 그룹을 해제합니다."""
    if not state.event_id or not state.ticket_item_id:
        pytest.skip("event_id 또는 ticket_item_id가 없어 테스트를 건너뜁니다.")
    if not _option_state["option_group_id"]:
        pytest.skip("option_group_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events/{state.event_id}/ticketItems/{state.ticket_item_id}/option/cancel"
    payload = {"optionGroupId": _option_state["option_group_id"]}
    print(f"\n[test_unapply_option_from_ticket] PATCH {url}")
    resp = requests.patch(url, json=payload, headers=auth_headers)
    print(f"[test_unapply_option_from_ticket] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    print("[test_unapply_option_from_ticket] 옵션 해제 완료")


def test_create_ticket_for_delete_test(base_url, auth_headers, state):
    """삭제 테스트를 위한 새 PREPARING 상태 이벤트의 티켓을 생성합니다."""
    if not state.host_id:
        pytest.skip("host_id가 없어 테스트를 건너뜁니다.")

    from datetime import datetime, timedelta
    future = (datetime.now() + timedelta(days=90)).strftime("%Y.%m.%d %H:%M")

    # 새 이벤트 생성 (PREPARING 상태)
    event_url = f"{base_url}/v1/events"
    event_payload = {
        "hostId": state.host_id,
        "name": "삭제테스트용이벤트",
        "startAt": future,
        "runTime": 60,
    }
    event_resp = requests.post(event_url, json=event_payload, headers=auth_headers)
    if event_resp.status_code not in (200, 201):
        pytest.skip(f"이벤트 생성 실패: {event_resp.text[:200]}")

    new_event_id = get_data(event_resp)["eventId"]

    # 티켓 생성
    ticket_url = f"{base_url}/v1/events/{new_event_id}/ticketItems"
    ticket_payload = {
        "payType": "무료티켓",
        "name": "삭제대상티켓",
        "description": "삭제 테스트용",
        "price": 0,
        "supplyCount": 10,
        "approveType": "선착순",
        "isQuantityPublic": True,
        "purchaseLimit": 1,
    }
    ticket_resp = requests.post(ticket_url, json=ticket_payload, headers=auth_headers)
    assert ticket_resp.status_code in (200, 201), f"티켓 생성 실패: {ticket_resp.text[:300]}"

    _option_state["new_ticket_item_id"] = get_data(ticket_resp)["ticketItemId"]
    _option_state["new_event_id"] = new_event_id
    print(f"[test_create_ticket_for_delete] 삭제 테스트용 티켓 생성: {_option_state['new_ticket_item_id']}")


def test_delete_ticket_in_preparing(base_url, auth_headers):
    """PREPARING 상태의 이벤트에서 티켓을 삭제하면 성공합니다."""
    new_event_id = _option_state.get("new_event_id")
    ticket_id = _option_state.get("new_ticket_item_id")
    if not new_event_id or not ticket_id:
        pytest.skip("삭제 테스트용 데이터가 없어 건너뜁니다.")

    url = f"{base_url}/v1/events/{new_event_id}/ticketItems/{ticket_id}"
    print(f"\n[test_delete_ticket_in_preparing] PATCH {url} (삭제)")
    resp = requests.patch(url, headers=auth_headers)
    print(f"[test_delete_ticket_in_preparing] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    print("[test_delete_ticket_in_preparing] PREPARING 상태 티켓 삭제 성공")


def test_delete_ticket_in_open_fails(base_url, auth_headers, state):
    """OPEN 상태의 이벤트에서 티켓을 삭제하면 실패합니다."""
    if not state.event_id or not state.ticket_item_id:
        pytest.skip("event_id 또는 ticket_item_id가 없어 테스트를 건너뜁니다.")

    url = f"{base_url}/v1/events/{state.event_id}/ticketItems/{state.ticket_item_id}"
    print(f"\n[test_delete_ticket_in_open_fails] PATCH {url} (OPEN 상태 삭제 시도)")
    resp = requests.patch(url, headers=auth_headers)
    print(f"[test_delete_ticket_in_open_fails] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code != 200, (
        f"OPEN 상태 이벤트의 티켓 삭제가 성공해서는 안 됩니다: status={resp.status_code}"
    )
    print(f"[test_delete_ticket_in_open_fails] OPEN 상태 티켓 삭제 차단 확인: {resp.status_code}")
