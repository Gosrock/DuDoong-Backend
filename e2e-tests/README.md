# DuDoong Backend E2E 테스트 스위트

외부 HTTP 요청으로 실행되는 엔드-투-엔드 테스트 스위트입니다. 로컬 개발 환경에서 백엔드 서버의 모든 주요 기능을 통합으로 검증합니다.

## 개요

### 테스트의 목적

- **실제 HTTP 통신**: Mock이 아닌 실제 API 엔드포인트로 검증
- **사용자 여정**: 로그인 → 호스트 → 이벤트 → 티켓 → 주문 → 발급 → 환불의 전체 플로우 검증
- **공유 상태 관리**: `TestState`를 통해 테스트 간 생성된 리소스(host_id, event_id 등)를 추적
- **외부 API 통합**: Kakao, Toss, Slack 등 외부 API 호출을 로컬 Mock 엔드포인트로 처리

### 왜 외부 HTTP를 사용하는가?

1. **통합 테스트**: 전체 요청/응답 사이클, 직렬화, 에러 처리를 검증
2. **배포 전 검증**: 실제 서버에서 동작하는 그대로 테스트
3. **엔드투엔드**: API 계층, 비즈니스 로직, 데이터베이스를 모두 포함한 흐름 검증

---

## 실행 방법

### 1) 서버 실행

로컬 개발 환경에서 백엔드 서버를 먼저 실행합니다.

```bash
# DuDoong-Backend 디렉토리에서
cd /Users/chanjin/Desktop/dudoong-v2/DuDoong-Backend

# API 서버 실행 (기본 포트: 8080)
./gradlew :DuDoong-Api:bootRun
```

또는 IDE에서 `DuDoong-Api` 모듈의 메인 클래스를 실행합니다.

**서버 준비 확인:**
```bash
curl http://localhost:8080/api/v1/auth/oauth/local/login
# 서버가 응답하면 테스트 실행 가능
```

### 2) Python 환경 설정

```bash
# e2e-tests 디렉토리로 이동
cd /Users/chanjin/Desktop/dudoong-v2/DuDoong-Backend/e2e-tests

# 필수 패키지 설치
pip install pytest requests

# 또는 requirements.txt가 있으면
pip install -r requirements.txt
```

### 3) E2E 테스트 실행

**전체 테스트 실행:**
```bash
# e2e-tests 디렉토리에서
pytest -v

# 또는 상세한 로그 출력
pytest -v -s
```

**특정 테스트 파일만 실행:**
```bash
# 인증 테스트만
pytest test_01_auth.py -v -s

# 주문 플로우 테스트만
pytest test_05_order_flow.py -v -s
```

**특정 테스트만 실행:**
```bash
pytest test_05_order_flow.py::test_create_order -v -s
```

**커스텀 API URL로 실행:**
```bash
# 기본값: http://localhost:8080/api
# 다른 URL에서 테스트하려면
API_BASE_URL=http://staging.dudoong.com/api pytest -v
```

### 4) 테스트 실행 순서

`conftest.py`의 `auth_token` 픽스처가 최초 1회 로그인을 수행하므로, 테스트는 다음 순서로 실행됩니다:

```
test_01_auth.py
├── test_local_login (선택사항: 이미 auth_token이 로그인함)
├── test_token_refresh
└── test_unauthenticated_access

test_02_host.py
├── test_create_host (host_id 저장)
└── test_read_host_profiles

test_03_event.py
├── test_create_event (event_id 저장)
├── test_update_event_basic
├── test_update_event_detail
├── test_search_events
└── test_read_event

test_04_ticket_item.py
├── test_create_free_ticket_item (ticket_item_id 저장)
├── test_get_event_ticket_items
└── test_open_event

test_05_order_flow.py
├── test_create_cart (cart_id 저장)
├── test_read_cart
├── test_create_order (order_uuid 저장)
├── test_free_order
└── test_read_order

test_06_issued_ticket.py
├── test_read_order_tickets
└── test_read_my_orders

test_07_comment.py
├── test_create_comment (comment_id 저장)
├── test_read_comments
└── test_get_comment_counts

test_08_refund.py
└── test_refund_order
```

---

## 테스트 시나리오 맵

### test_01_auth.py - 인증 시나리오

| 테스트 | 설명 | 인증 | 목적 |
|--------|------|------|------|
| `test_local_login` | 로컬 개발 로그인 | X | 로그인 후 accessToken/refreshToken 획득 |
| `test_token_refresh` | 토큰 갱신 | X | refreshToken으로 새로운 accessToken 발급 |
| `test_unauthenticated_access` | 미인증 접근 차단 | X | 토큰 없이 보호된 엔드포인트 접근 시 401/403 반환 |

### test_02_host.py - 호스트 시나리오

| 테스트 | 설명 | 인증 | 의존 | 생성 리소스 |
|--------|------|------|------|------------|
| `test_create_host` | 호스트 생성 | O | - | host_id |
| `test_read_host_profiles` | 내 호스트 목록 조회 | O | host_id | - |

**핵심 검증:**
- 호스트 정보 정상 저장
- 생성된 호스트가 목록에 포함되는지 확인

### test_03_event.py - 이벤트 시나리오

| 테스트 | 설명 | 인증 | 의존 | 생성 리소스 |
|--------|------|------|------|------------|
| `test_create_event` | 이벤트 생성 | O | host_id | event_id |
| `test_update_event_basic` | 기본 정보 수정 | O | event_id | - |
| `test_update_event_detail` | 상세 정보 수정 | O | event_id | - |
| `test_search_events` | 이벤트 검색 | X | - | - |
| `test_read_event` | 이벤트 상세 조회 | X | event_id | - |

**핵심 검증:**
- 이벤트 생성 및 ID 발급
- 기본 정보(이름, 시작시각, 장소) 수정
- 상세 정보(포스터, 설명) 수정
- 키워드 검색 기능
- 공개 조회 가능

### test_04_ticket_item.py - 티켓 상품 시나리오

| 테스트 | 설명 | 인증 | 의존 | 생성 리소스 |
|--------|------|------|------|------------|
| `test_create_free_ticket_item` | 무료 티켓 생성 | O | event_id | ticket_item_id |
| `test_get_event_ticket_items` | 티켓 상품 목록 조회 | X | event_id | - |
| `test_open_event` | 이벤트 오픈 | O | event_id, ticket_item_id | - |

**핵심 검증:**
- payType: 무료티켓, approveType: 선착순으로 생성
- 생성된 티켓이 목록에 포함되는지 확인
- 기본 정보 + 상세 정보 + 티켓 상품 모두 존재할 때만 오픈 가능

### test_05_order_flow.py - 주문 플로우 (핵심 시나리오)

| 테스트 | 설명 | 인증 | 의존 | 생성 리소스 |
|--------|------|------|------|------------|
| `test_create_cart` | 장바구니 생성 | O | ticket_item_id | cart_id |
| `test_read_cart` | 장바구니 조회 | O | - | - |
| `test_create_order` | 주문 생성 | O | cart_id | order_uuid |
| `test_free_order` | 무료 주문 완료 | O | order_uuid | - |
| `test_read_order` | 주문 상세 조회 | O | order_uuid | - |

**핵심 검증:**
- 티켓 상품을 장바구니에 추가
- 장바구니를 주문서로 변환
- 무료 주문 완료(0원 결제)
- 주문 조회 시 발급 티켓 포함 확인

### test_06_issued_ticket.py - 발급 티켓 시나리오

| 테스트 | 설명 | 인증 | 의존 | 목적 |
|--------|------|------|------|------|
| `test_read_order_tickets` | 주문 내 티켓 목록 조회 | O | order_uuid | 발급된 티켓 상세 정보 확인 |
| `test_read_my_orders` | 마이페이지 예매 목록 | O | order_uuid | 현재 유효한 예매 목록 조회 |

**핵심 검증:**
- 주문 완료 후 티켓이 정상적으로 발급되었는지 확인
- 마이페이지에서 예매 목록 조회(showing=true)

### test_07_comment.py - 응원톡(댓글) 시나리오

| 테스트 | 설명 | 인증 | 의존 | 생성 리소스 |
|--------|------|------|------|------------|
| `test_create_comment` | 응원글 생성 | O | event_id | comment_id |
| `test_read_comments` | 응원글 목록 조회 | X | event_id | - |
| `test_get_comment_counts` | 응원글 개수 조회 | X | event_id | - |

**핵심 검증:**
- nickName 1~10자, content 1~150자 제약 확인
- 생성된 응원글이 목록에 포함
- 응원글 개수 카운트

### test_08_refund.py - 환불 시나리오

| 테스트 | 설명 | 인증 | 의존 | 생성 리소스 |
|--------|------|------|------|------------|
| `test_refund_order` | 주문 환불 | O | ticket_item_id | refund_order_uuid |

**핵심 검증:**
- 새로운 주문 생성 → 무료 결제 → 환불 요청
- 환불 후 orderId 반환 확인
- 기존 order_uuid와 독립적으로 처리

---

## 테스트 커버리지 매트릭스

### 기능별 커버리지

| 기능 | 테스트 | 상태 |
|------|--------|------|
| **인증** | 로그인, 토큰 갱신, 미인증 접근 | ✓ 완료 |
| **호스트** | 생성, 목록 조회, 초대/가입/거절, 역할변경, Slack URL, 권한검증 | ✓ 완료 |
| **이벤트** | 생성, 기본/상세 수정, 검색, 상세 조회, 오픈 | ✓ 완료 |
| **티켓 상품** | 생성(무료/두둥), 목록 조회, 옵션 CRUD, 삭제, 재고 검증 | ✓ 완료 |
| **장바구니** | 생성, 조회 | ✓ 완료 |
| **주문** | 생성, 무료 결제, 상세 조회, 승인/거절, 취소, 쿠폰 적용 | ✓ 완료 |
| **발급 티켓** | 목록 조회, 예매 목록 조회 | ✓ 완료 |
| **응원톡** | 생성, 목록 조회, 개수 조회 | ✓ 완료 |
| **환불** | 환불 요청, 이중 환불 방지, 재고 복원, 티켓 취소 상태 | ✓ 완료 |
| **쿠폰** | 캠페인 생성, 발급, 적용 주문, 목록 조회 | ✓ 완료 |
| **관리자** | 주문/티켓 테이블 조회, 입장 처리, 이중 입장 차단 | ✓ 완료 |
| **엣지 케이스** | 재고 소진, 타유저 조회 차단, 중복 결제, 미인증 | ✓ 완료 |

### API 엔드포인트 커버리지

| 엔드포인트 | 메서드 | 테스트 | 상태 |
|-----------|--------|--------|------|
| `/v1/auth/oauth/local/login` | POST | test_local_login | ✓ |
| `/v1/auth/token/refresh` | POST | test_token_refresh | ✓ |
| `/v1/hosts` | POST | test_create_host | ✓ |
| `/v1/hosts` | GET | test_read_host_profiles | ✓ |
| `/v1/events` | POST | test_create_event | ✓ |
| `/v1/events/{eventId}/basic` | PATCH | test_update_event_basic | ✓ |
| `/v1/events/{eventId}/details` | PATCH | test_update_event_detail | ✓ |
| `/v1/events/search` | GET | test_search_events | ✓ |
| `/v1/events/{eventId}` | GET | test_read_event | ✓ |
| `/v1/events/{eventId}/ticketItems` | POST | test_create_free_ticket_item | ✓ |
| `/v1/events/{eventId}/ticketItems` | GET | test_get_event_ticket_items | ✓ |
| `/v1/events/{eventId}/open` | PATCH | test_open_event | ✓ |
| `/v1/carts` | POST | test_create_cart | ✓ |
| `/v1/carts/recent` | GET | test_read_cart | ✓ |
| `/v1/orders/` | POST | test_create_order | ✓ |
| `/v1/orders/{orderUuid}/free` | POST | test_free_order | ✓ |
| `/v1/orders/{orderUuid}` | GET | test_read_order | ✓ |
| `/v1/orders/{orderUuid}/tickets` | GET | test_read_order_tickets | ✓ |
| `/v1/orders` | GET | test_read_my_orders | ✓ |
| `/v1/events/{eventId}/comments` | POST | test_create_comment | ✓ |
| `/v1/events/{eventId}/comments` | GET | test_read_comments | ✓ |
| `/v1/events/{eventId}/comments/counts` | GET | test_get_comment_counts | ✓ |
| `/v1/orders/{orderUuid}/refund` | POST | test_refund_order | ✓ |

---

## 공유 상태 흐름 (TestState)

### 상태 관리 구조

`conftest.py`의 `TestState` 클래스는 **세션 스코프 픽스처**로 전체 테스트 실행 중 단 하나의 인스턴스만 유지됩니다.

```python
class TestState:
    access_token: str = ""           # 로그인 토큰
    refresh_token: str = ""          # 토큰 갱신용
    host_id: int = 0                 # 호스트 ID
    event_id: int = 0                # 이벤트 ID
    ticket_item_id: int = 0          # 티켓 상품 ID
    cart_id: int = 0                 # 장바구니 ID
    order_uuid: str = ""             # 주문 UUID
    comment_id: int = 0              # 응원글 ID
    refund_order_uuid: str = ""      # 환불 대상 주문 UUID
```

### 데이터 전달 흐름

```
test_01_auth
  ↓ state.access_token, state.refresh_token 저장
test_02_host
  ↓ state.host_id 저장
test_03_event
  ↓ state.event_id 저장
test_04_ticket_item
  ↓ state.ticket_item_id 저장 + event 오픈
test_05_order_flow (핵심 플로우)
  ↓ state.cart_id → state.order_uuid 저장
test_06_issued_ticket
  ↓ order_uuid로 발급 티켓 조회
test_07_comment
  ↓ state.comment_id 저장
test_08_refund
  ↓ 새로운 주문 생성 후 환불

test_09~18: 에러/라이프사이클/멀티유저/재고/CRUD/상태 매트릭스

test_19_host_invite
  ↓ 호스트 초대/가입/거절/역할/Slack URL
test_20_ticket_options
  ↓ 옵션 그룹 CRUD, 티켓 적용/해제, 삭제
test_21_dudoong_ticket
  ↓ 두둥티켓 승인/거절 플로우
test_22_order_detail_verification
  ↓ 주문 응답 필드 상세 검증
test_23_refund_edge_cases
  ↓ 이중 환불, 재고 복원, 티켓 취소 상태
test_24_admin_features
  ↓ 관리자 주문/티켓 테이블, 입장 처리, 이중 입장
test_25_coupon_flow
  ↓ 쿠폰 캠페인/발급/적용
test_26_order_edge_cases
  ↓ 재고 소진, 타유저 조회 차단, 중복 결제, 미인증
```

### 의존성 검증

각 테스트는 필요한 리소스의 존재 여부를 확인합니다:

```python
def test_create_event(base_url, auth_headers, state):
    assert state.host_id, "host_id가 없습니다. test_02_host를 먼저 실행하세요."
    # host_id가 없으면 AssertionError로 테스트 실패
```

---

## 외부 API 처리

### 로컬 개발 환경의 API 모킹

실제 외부 API를 호출하지 않고 로컬에서 즉시 응답하기 위해 다음과 같이 처리됩니다:

#### 1) Kakao OAuth 로그인
- **엔드포인트**: `POST /v1/auth/oauth/local/login`
- **방식**: Spring Security 설정에서 `LocalAuthController` 제공
- **역할**: 실제 Kakao와 통신하지 않고 테스트 사용자 정보로 즉시 로그인
- **요청**:
  ```json
  {
    "email": "test@dudoong.com",
    "name": "E2E테스터",
    "phoneNumber": "010-0000-0000",
    "profileImage": null,
    "marketingAgree": false
  }
  ```
- **응답**:
  ```json
  {
    "status": 200,
    "data": {
      "accessToken": "eyJ0eXAiOiJKV1QiLC...",
      "refreshToken": "eyJ0eXAiOiJKV1QiLC..."
    }
  }
  ```

#### 2) Toss Payments (결제)
- **호출 지점**: 유료 결제 시 (현재 테스트에는 무료 결제만 사용)
- **로컬 처리**: 무료 주문은 `/v1/orders/{orderUuid}/free` 엔드포인트로 처리
- **외부 API 호출 안 함**: E2E 테스트는 무료 티켓만 생성하므로 Toss API 호출 없음

#### 3) AWS S3 (이미지 업로드)
- **호출 지점**: 이벤트 포스터 업로드 시
- **현재 테스트**: 이미지 키만 저장 (실제 파일 업로드 안 함)
- **향후**: `ImageController` 프리사인드 URL 엔드포인트로 확장 가능

#### 4) Slack 알림
- **호출 지점**: 정산 완료, 예매 안내 등
- **로컬 처리**: Slack 설정이 활성화되지 않거나 Mock으로 처리
- **E2E 테스트**: Slack 호출 없음 (선택사항)

#### 5) NCP AlimTalk (문자 알림)
- **호출 지점**: 예매 확정, 입장 안내 등
- **로컬 처리**: NCP API 설정이 비활성화됨
- **E2E 테스트**: AlimTalk 호출 없음

### 로컬 테스트 실행 시 주의사항

- **데이터베이스**: 로컬 MySQL 필수 (테스트마다 새로운 데이터 생성)
- **Redis**: Optional (캐시/락 기능이 필요한 경우)
- **외부 API 토큰**: application.properties에서 Mock 설정 확인

---

## 헬퍼 함수 및 유틸리티

### conftest.py 제공 함수

#### `assert_status(response, expected_status)`
HTTP 응답 상태 코드를 검증합니다.
```python
assert_status(resp, 200)  # 200이 아니면 AssertionError + 응답 본문 출력
```

#### `get_data(response)`
`SuccessResponseAdvice` 래핑된 응답에서 `data` 필드를 추출합니다.
```python
resp = requests.get(url)
data = get_data(resp)
# {"status": 200, "data": {...}} → {...}
```

### 응답 형식

모든 API 응답은 `SuccessResponseAdvice`에 의해 다음과 같이 래핑됩니다:

```json
{
  "status": 200,
  "data": {
    "hostId": 1,
    "name": "E2E테스트호스트",
    ...
  }
}
```

테스트에서는 항상 `get_data(response)`로 `data` 필드를 추출합니다.

---

## 추가 예정 (미커버 시나리오)

#### 1) 유료 주문 (Toss Payments)
- [ ] 유료 티켓 상품 생성 + Toss 결제 승인 콜백

#### 2) 호스트 정산
- [ ] 정산 레포트 조회 / 기간별 집계

#### 3) 이벤트 카테고리
- [ ] 카테고리별 / 다중 카테고리 검색

---

## 테스트 작성 가이드

### 새로운 테스트 추가 시 체크리스트

1. **파일명**: `test_NN_<domain>.py` 형식 (NN: 실행 순서)
2. **상태 의존성**: `state`에서 필요한 리소스 확인
3. **Fixture 사용**: `base_url`, `auth_headers`, `state` 활용
4. **응답 검증**: `assert_status()` + `get_data()` 조합
5. **로깅**: 테스트 진행 상황을 `print()`로 출력
6. **상태 저장**: 생성된 리소스는 `state`에 저장 (다른 테스트에서 재사용)

### 예시 테스트

```python
def test_create_something(base_url, auth_headers, state):
    """설명: 무엇을 테스트하는가"""
    # 의존성 확인
    assert state.event_id, "event_id가 필요합니다."

    # API 호출
    url = f"{base_url}/v1/something"
    payload = {...}
    print(f"\n[test_create_something] POST {url}")
    resp = requests.post(url, json=payload, headers=auth_headers)
    print(f"[test_create_something] status={resp.status_code}")

    # 응답 검증
    assert_status(resp, 200)
    data = get_data(resp)
    assert "someId" in data

    # 상태 저장 (다음 테스트에서 사용)
    state.some_id = data["someId"]
    print(f"[test_create_something] 생성 완료: id={state.some_id}")
```

---

## 트러블슈팅

### 문제: "host_id가 없습니다"
**원인**: test_02_host를 실행하지 않았음
**해결**: 전체 테스트를 순서대로 실행 (`pytest -v`)

### 문제: "connect refused" 또는 "Connection refused"
**원인**: 백엔드 서버가 실행 중이지 않음
**해결**:
```bash
# 서버 실행 확인
curl http://localhost:8080/api/v1/auth/oauth/local/login

# 서버 재시작
./gradlew :DuDoong-Api:bootRun
```

### 문제: "API_BASE_URL을 찾을 수 없다"
**원인**: 커스텀 URL을 설정하지 않음
**해결**: 환경 변수 설정 또는 conftest.py의 기본값 확인
```bash
API_BASE_URL=http://localhost:8080/api pytest -v
```

### 문제: "accessToken이 유효하지 않음" (401)
**원인**: 토큰 만료 또는 로그인 실패
**해결**: 전체 테스트 재실행 (auth_token 픽스처가 새로 로그인)

---

## 참고 자료

- **API 문서**: SwaggerUI (http://localhost:8080/swagger-ui.html)
- **백엔드 코드**: `/DuDoong-Backend/`
- **Domain 구조**: `/DuDoong-Backend/DuDoong-Domain/src/main/kotlin/`
- **API 컨트롤러**: `/DuDoong-Backend/DuDoong-Api/src/main/kotlin/`
