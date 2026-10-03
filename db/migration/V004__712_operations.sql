-- V004__712_operations.sql
-- 이슈: Gosrock/DuDoong-Backend#712
-- 목적: v2 공연 운영 (주문 거절 사유 종류, 셀프 체크인 QR 토큰)
--   - tbl_order.refuse_reason_type: v2 거절 사유 종류 (DEPOSIT_UNCONFIRMED / AMOUNT_MISMATCH / SOLD_OUT / ETC). 표시 문구는 기존 cancel_reason 에 함께 기록
--     v1 거절 건은 NULL 로 두고 조회 시 분류한다 (CANCELED + approved_at NULL = 거절) → [DATA] 없음
--   - tbl_event.check_in_token: 공연별 고정 셀프 체크인 토큰 (DEC-011). 공연 등록 시가 아니라 Q-4 최초 조회 시 앱이 조건부 UPDATE 로 생성, unique
--   - 인덱스 (baseline 에 없음, prod 2026-10-04 주문 약 6.4만·발급 티켓 약 7.3만 행)
--     tbl_order(event_id, order_status): 공연별 주문 목록·상태별 건수·대시보드·환불 목록
--     tbl_issued_ticket(event_id, user_id): 공연별 발급 티켓 목록·입장 통계, 셀프 체크인 본인 티켓 조회
--     tbl_issued_ticket(uuid) UNIQUE: QR 스캔(v1/v2 입장)·티켓 상세 단건 조회 (없으면 전체 스캔, prod 73,086행 uuid 전부 고유 2026-10-04)
--     tbl_issued_ticket(order_uuid): 주문의 발급 티켓 조회 (v1 승인·취소의 티켓 철회, v2 주문 상세). 주문당 여러 장이라 일반 인덱스
--     tbl_order(uuid) UNIQUE: 승인·거절·취소·상세 등 주문 단건 조회 (prod 63,773행 uuid 전부 고유 2026-10-04)
-- v1 영향: 없음 (nullable 컬럼·인덱스 추가만. v1 앱은 두 컬럼을 읽지도 쓰지도 않는다. v1 의 공연별 주문·발급 티켓 조회도 같은 인덱스로 빨라질 수 있음)
--   - INSTANT 컬럼 추가는 메타데이터만 바꿔 테이블 재작성·잠금 없음. 인덱스는 온라인 DDL(INPLACE, LOCK=NONE)로 쓰기 차단 없음
-- 실행 전 확인: 아래 두 쿼리가 0건이어야 한다 (중복이 있으면 UNIQUE 인덱스 생성이 실패하므로 원인 확인 후 진행)
--   SELECT uuid, COUNT(*) FROM tbl_issued_ticket GROUP BY uuid HAVING COUNT(*) > 1;
--   SELECT uuid, COUNT(*) FROM tbl_order GROUP BY uuid HAVING COUNT(*) > 1;
-- 실행 순서: (1) 위 중복 확인 0건 → (2) 이 파일 DDL → (3) 앱 배포. [DATA] 없음
-- 실행 기록:
--   staging: YYYY-MM-DD (실행자)
--   prod:    YYYY-MM-DD (실행자)

SET NAMES utf8mb4;

ALTER TABLE tbl_order ADD COLUMN refuse_reason_type VARCHAR(30) NULL, ALGORITHM=INSTANT;

ALTER TABLE tbl_event ADD COLUMN check_in_token VARCHAR(64) NULL, ALGORITHM=INSTANT;

ALTER TABLE tbl_event ADD UNIQUE INDEX uk_event_check_in_token (check_in_token), ALGORITHM=INPLACE, LOCK=NONE;

ALTER TABLE tbl_order ADD INDEX idx_order_event_id_status (event_id, order_status), ALGORITHM=INPLACE, LOCK=NONE;

ALTER TABLE tbl_issued_ticket ADD INDEX idx_issued_ticket_event_id_user_id (event_id, user_id), ALGORITHM=INPLACE, LOCK=NONE;

ALTER TABLE tbl_issued_ticket ADD UNIQUE INDEX uk_issued_ticket_uuid (uuid), ALGORITHM=INPLACE, LOCK=NONE;

ALTER TABLE tbl_order ADD UNIQUE INDEX uk_order_uuid (uuid), ALGORITHM=INPLACE, LOCK=NONE;

ALTER TABLE tbl_issued_ticket ADD INDEX idx_issued_ticket_order_uuid (order_uuid), ALGORITHM=INPLACE, LOCK=NONE;
