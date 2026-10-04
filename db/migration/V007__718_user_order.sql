-- V007__718_user_order.sql
-- 이슈: Gosrock/DuDoong-Backend#718
-- 목적: v2 사용자 앱 주문·주문내역·취소/환불 (O-1 ~ O-4)
--   - tbl_order.depositor_name: 두둥티켓 주문의 입금자명 (주문 시점 값, 닉네임 변경과 무관 — DEC-022). 앞뒤 공백 제외 1~20자
--   - tbl_order.payment_channel: v2 결제 방식 BANK_TRANSFER / TOSS_TRANSFER / FREE (토스 송금도 계좌송금, 백엔드는 구분 기록만 — DEC-022)
--     v1 으로 만든 주문은 두 컬럼 모두 NULL (v1 코드는 읽지도 쓰지도 않는다)
--   - tbl_order_refund_account: 사용자 취소·환불 요청 때 입력한 환불 받을 계좌 (주문 1:1, order_id UNIQUE).
--     유료(두둥티켓) 주문 취소만 저장. 계좌번호는 원문 저장, 노출은 호스트 매니저 이상(전체)·주문자 본인(뒤 4자리)
--   - idx_order_user_id_id (user_id, order_id): 내 주문 목록 WHERE user_id = ? ORDER BY order_id DESC (v2 O-2, v1 마이페이지 GET /v1/orders 도 같은 조건)
--     + v2 중복 주문 확인(같은 사용자 최근 10초). 기존에는 user_id 인덱스가 없어 전체 스캔
-- v1 영향: 없음 (nullable 컬럼·테이블·인덱스 추가만). v2 사용자 취소는 상태값을 v1 사용자 환불과 같은 REFUND 로 남긴다
-- 실행 순서: (1) 이 파일 DDL → (2) 앱 배포. [DATA] 없음
--   - 새 앱은 tbl_order 의 새 컬럼을 매핑하므로 DDL 이 없으면 주문 조회 전체가 실패한다 → 반드시 DDL 먼저
-- 실행 기록:
--   staging: YYYY-MM-DD (실행자)
--   prod:    YYYY-MM-DD (실행자)

SET NAMES utf8mb4;

ALTER TABLE tbl_order ADD COLUMN depositor_name VARCHAR(20) NULL, ADD COLUMN payment_channel VARCHAR(20) NULL, ALGORITHM=INSTANT;

ALTER TABLE tbl_order ADD INDEX idx_order_user_id_id (user_id, order_id), ALGORITHM=INPLACE, LOCK=NONE;

CREATE TABLE `tbl_order_refund_account` (
  `order_refund_account_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `order_id` bigint NOT NULL,
  `bank_name` varchar(20) NOT NULL,
  `account_holder` varchar(20) NOT NULL,
  `account_number` varchar(30) NOT NULL,
  PRIMARY KEY (`order_refund_account_id`),
  UNIQUE KEY `uk_order_refund_account_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
