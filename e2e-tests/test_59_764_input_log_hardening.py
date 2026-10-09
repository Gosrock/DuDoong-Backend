"""
입력·외부 호출·로그 보강 E2E 테스트 (#764).

- 요청 제한 IP 판정: 클라이언트가 보낸 Forwarded·X-Forwarded-For 왼쪽 값으로 화이트리스트(127.0.0.1)를 통과하지 못한다.
  E2E 는 127.0.0.1 에서 접속하므로 X-Forwarded-For 를 붙이면 "nginx 가 전달한 요청" 과 같다.
  서버의 요청 제한을 낮춰 띄웠을 때만 돈다: 서버 RATE_LIMIT_OVERDRAFT=N·RATE_LIMIT_REFILL=N, pytest E2E_RATE_LIMIT_OVERDRAFT=N
- presigned URL 발급은 유저별 분당 30회 (서버 기본값)
- 호스트 슬랙 URL 은 Slack Incoming Webhook 형식만, 회원가입 프로필 이미지는 카카오 CDN 주소만
- X-Trace-Id 형식 검증, 응원톡 랜덤 limit 상한, 스프링 기본 예외 응답 문구

재실행해도 충돌하지 않도록 유저 이메일·IP 에 실행마다 다른 값을 쓴다. DB 직접 접근은 하지 않는다.
"""
import os
import random
import re
import uuid

import pytest
import requests

from conftest import assert_status, get_data

RUN = uuid.uuid4().hex[:8]
KAKAO_IMAGE = "http://k.kakaocdn.net/dn/e2e764/img_640x640.jpg"
PRESIGNED_PER_MINUTE = 30


def _login(base_url, who, profile_image=None):
    return requests.post(
        f"{base_url}/v1/auth/oauth/local/login",
        json={"email": f"e2e764-{who}-{RUN}@dudoong.com", "name": f"보강{who}", "phoneNumber": "010-0000-0000",
              "profileImage": profile_image, "marketingAgree": False},
    )


def _token(base_url, who, profile_image=None):
    resp = _login(base_url, who, profile_image)
    assert_status(resp, 200)
    return get_data(resp)["accessToken"]


def _h(token):
    return {"Authorization": f"Bearer {token}"}


def _random_ip():
    """문서·벤치마크용 대역(198.18.0.0/15). 실행마다 다른 IP 라 Redis 버킷이 겹치지 않는다"""
    return f"198.18.{random.randint(0, 255)}.{random.randint(1, 254)}"


def _health(base_url, times, headers=None):
    return [requests.get(f"{base_url}/v2/health", headers=headers or {}).status_code for _ in range(times)]


@pytest.fixture(scope="module")
def limit():
    value = os.environ.get("E2E_RATE_LIMIT_OVERDRAFT")
    if not value:
        pytest.skip("요청 제한 E2E 는 서버를 RATE_LIMIT_OVERDRAFT·RATE_LIMIT_REFILL 을 낮춰 띄우고 E2E_RATE_LIMIT_OVERDRAFT 로 같은 값을 줄 때만 실행")
    return int(value)


# ===== 요청 제한 IP 판정 (H1) =====

def test_01_client_sent_forwarded_for_not_used(base_url, limit):
    statuses = _health(base_url, limit + 2, {"X-Forwarded-For": f"127.0.0.1, {_random_ip()}"})
    assert statuses[:limit] == [200] * limit, statuses
    assert statuses[limit:] == [429, 429], statuses


def test_02_forwarded_header_ignored(base_url, limit):
    statuses = _health(base_url, limit + 2, {"Forwarded": "for=127.0.0.1", "X-Forwarded-For": _random_ip()})
    assert statuses[limit:] == [429, 429], statuses


def test_03_429_body_is_standard_error(base_url, limit):
    headers = {"X-Forwarded-For": _random_ip()}
    _health(base_url, limit, headers)
    resp = requests.get(f"{base_url}/v2/health", headers=headers)
    assert_status(resp, 429)
    assert resp.json()["code"] == "GLOBAL_429_1"


def test_04_direct_loopback_is_whitelisted(base_url, limit):
    assert _health(base_url, limit + 3) == [200] * (limit + 3)


# ===== presigned URL 발급 횟수 (M4) =====

def test_05_presigned_url_rate_limited_per_user(base_url):
    token = _token(base_url, "presigned")
    statuses = [requests.post(f"{base_url}/v2/me/images", json={"extension": "PNG"}, headers=_h(token)).status_code
                for _ in range(PRESIGNED_PER_MINUTE + 10)]
    assert statuses[:PRESIGNED_PER_MINUTE] == [200] * PRESIGNED_PER_MINUTE, statuses
    # 버킷은 분당 30개를 조금씩(greedy) 채우므로 호출하는 몇 초 동안 몇 개가 더 통과할 수 있다
    assert 429 in statuses and statuses.count(200) <= PRESIGNED_PER_MINUTE + 5, statuses
    # 다른 유저는 따로 센다
    other = _token(base_url, "presigned-other")
    assert_status(requests.post(f"{base_url}/v2/me/images", json={"extension": "PNG"}, headers=_h(other)), 200)


# ===== 입력 형식 (M1·L1) =====

def test_06_host_slack_url_must_be_slack_webhook(base_url):
    token = _token(base_url, "slack")
    resp = requests.post(f"{base_url}/v1/hosts", json={"name": f"보강{RUN[:5]}", "contactEmail": "h@dudoong.com", "contactNumber": "010-1234-5678"},
                         headers=_h(token))
    assert resp.status_code in (200, 201), resp.text[:300]
    host_id = get_data(resp)["hostId"]
    for url in ["https://slack.dd.com", "https://hooks.slack.com.example.com/services/T0/B0/x"]:
        resp = requests.patch(f"{base_url}/v1/hosts/{host_id}/slack", json={"slackUrl": url}, headers=_h(token))
        assert_status(resp, 400)
    detail = get_data(requests.get(f"{base_url}/v1/hosts/{host_id}", headers=_h(token)))
    assert not detail.get("slackUrl"), detail


def test_07_register_profile_image_kakao_only(base_url):
    assert_status(_login(base_url, "other-image", "https://example.com/kakao.png"), 400)
    token = _token(base_url, "kakao-image", KAKAO_IMAGE)
    me = get_data(requests.get(f"{base_url}/v1/users/me", headers=_h(token)))
    assert me["profileImage"] == KAKAO_IMAGE, me


# ===== 로그·오류 응답·상한 (L3·L4·L6) =====

def test_08_trace_id_format(base_url):
    assert requests.get(f"{base_url}/v2/health", headers={"X-Trace-Id": "e2e-764-trace"}).headers["X-Trace-Id"] == "e2e-764-trace"
    replaced = requests.get(f"{base_url}/v2/health", headers={"X-Trace-Id": "bad id <x>"}).headers["X-Trace-Id"]
    assert re.fullmatch(r"[0-9a-f]{8}", replaced), replaced


def test_09_comment_random_limit_cap(base_url):
    token = _token(base_url, "comment")
    resp = requests.get(f"{base_url}/v1/events/1/comments/random", params={"limit": 51}, headers=_h(token))
    assert_status(resp, 400)
    assert "limit 값은 50 이하여야 합니다." in resp.json()["reason"]
    resp = requests.get(f"{base_url}/v1/events/1/comments/random", params={"limit": 50}, headers=_h(token))
    assert resp.status_code != 400, resp.text[:300]


def test_10_framework_error_reason_is_generic(base_url):
    token = _token(base_url, "error")
    resp = requests.get(f"{base_url}/v1/events/abc/comments/counts", headers=_h(token))
    assert_status(resp, 400)
    assert resp.json()["reason"] == "Bad Request", resp.text[:300]
    resp = requests.post(f"{base_url}/v1/events/1/comments", data='{"content": ', headers={**_h(token), "Content-Type": "application/json"})
    assert_status(resp, 400)
    assert resp.json()["reason"] == "Bad Request", resp.text[:300]
