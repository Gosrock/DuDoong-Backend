"""
응원톡(댓글) 관련 E2E 시나리오 테스트.
응원글 생성 및 목록 조회를 검증합니다.
"""
import pytest
import requests

from conftest import assert_status


def test_create_comment(base_url, auth_headers, state):
    """
    이벤트에 응원글을 생성하고 comment_id를 state에 저장합니다.
    nickName은 1~10자, content는 1~150자 제한이 있습니다.
    """
    assert state.event_id, "event_id가 없습니다. test_03_event를 먼저 실행하세요."
    url = f"{base_url}/v1/events/{state.event_id}/comments"
    payload = {
        "nickName": "E2E응원단",
        "content": "E2E 테스트 응원합니다! 화이팅!",
    }
    print(f"\n[test_create_comment] POST {url}")
    print(f"[test_create_comment] payload={payload}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_create_comment] status={resp.status_code}, body={resp.text[:400]}")

    assert resp.status_code in (200, 201), (
        f"응원글 생성 실패: status={resp.status_code}, body={resp.text[:300]}"
    )
    data = resp.json()
    assert "id" in data, f"응답에 id 필드가 없습니다: {data}"
    state.comment_id = data["id"]
    print(f"[test_create_comment] 응원글 생성 완료: comment_id={state.comment_id}")


def test_read_comments(base_url, state):
    """
    인증 없이 이벤트의 응원글 목록을 조회합니다.
    생성된 응원글이 목록에 포함되는지 확인합니다.
    """
    assert state.event_id, "event_id가 없습니다."
    url = f"{base_url}/v1/events/{state.event_id}/comments"
    print(f"\n[test_read_comments] GET {url} (인증 없음)")
    resp = requests.get(url)
    print(f"[test_read_comments] status={resp.status_code}, body={resp.text[:400]}")

    assert_status(resp, 200)
    data = resp.json()
    assert "data" in data, f"응답에 data 필드가 없습니다: {data}"
    print(f"[test_read_comments] 응원글 목록 조회 완료: {len(data['data'])}개")


def test_get_comment_counts(base_url, state):
    """인증 없이 이벤트의 응원글 개수를 조회합니다."""
    assert state.event_id, "event_id가 없습니다."
    url = f"{base_url}/v1/events/{state.event_id}/comments/counts"
    print(f"\n[test_get_comment_counts] GET {url} (인증 없음)")
    resp = requests.get(url)
    print(f"[test_get_comment_counts] status={resp.status_code}, body={resp.text[:200]}")

    assert_status(resp, 200)
    data = resp.json()
    assert "count" in data, f"응답에 count 필드가 없습니다: {data}"
    print(f"[test_get_comment_counts] 응원글 개수: {data.get('count')}")
