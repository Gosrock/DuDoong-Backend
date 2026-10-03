-- V004__712_operations.sql
-- 이슈: Gosrock/DuDoong-Backend#712
-- 목적: v2 공연 운영 (주문 거절 사유 종류, 셀프 체크인 QR 토큰)
--   - tbl_order.refuse_reason_type: v2 거절 사유 종류 (DEPOSIT_UNCONFIRMED / AMOUNT_MISMATCH / SOLD_OUT / ETC). 표시 문구는 기존 cancel_reason 에 함께 기록
--     v1 거절 건은 NULL 로 두고 조회 시 분류한다 (CANCELED + approved_at NULL = 거절) → [DATA] 없음
--   - tbl_event.check_in_token: 공연별 고정 셀프 체크인 토큰 (DEC-011). 최초 조회 시 앱이 조건부 UPDATE 로 생성, unique
-- v1 영향: 없음 (nullable 컬럼·인덱스 추가만. v1 앱은 두 컬럼을 읽지도 쓰지도 않는다)
--   - INSTANT 컬럼 추가는 메타데이터만 바꿔 테이블 재작성·잠금 없음. 인덱스는 온라인 DDL(INPLACE, LOCK=NONE)이며 기존 행은 전부 NULL
-- 실행 순서: (1) 이 파일 DDL → (2) 앱 배포. [DATA] 없음
-- 실행 기록:
--   staging: YYYY-MM-DD (실행자)
--   prod:    YYYY-MM-DD (실행자)

SET NAMES utf8mb4;

ALTER TABLE tbl_order ADD COLUMN refuse_reason_type VARCHAR(30) NULL, ALGORITHM=INSTANT;

ALTER TABLE tbl_event ADD COLUMN check_in_token VARCHAR(64) NULL, ALGORITHM=INSTANT;

ALTER TABLE tbl_event ADD UNIQUE INDEX uk_event_check_in_token (check_in_token), ALGORITHM=INPLACE, LOCK=NONE;
