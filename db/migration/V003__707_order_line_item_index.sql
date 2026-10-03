-- V003__707_order_line_item_index.sql
-- 이슈: Gosrock/DuDoong-Backend#707
-- 목적: 티켓(item)별 진행 중(승인 대기) 주문 존재 조회용 인덱스. v2 티켓 잠금 판정(관리 목록·수정·옵션)에서 사용
--   - 쿼리: SELECT DISTINCT l.item_id FROM tbl_order o JOIN tbl_order_line l ON l.order_id = o.order_id
--           WHERE l.item_id IN (...) AND o.order_status = 'PENDING_APPROVE'
--   - 인덱스 없으면 tbl_order_line 풀스캔 (prod 2026-10-04 기준 약 6.4만 행, EXPLAIN type=ALL)
-- v1 영향: 없음 (인덱스 추가만). 온라인 DDL(INPLACE, LOCK=NONE) 로 쓰기 차단 없음
-- 실행 순서: (1) 이 파일 DDL → (2) 앱 배포. [DATA] 없음
-- 실행 기록:
--   staging: YYYY-MM-DD (실행자)
--   prod:    YYYY-MM-DD (실행자)

SET NAMES utf8mb4;

ALTER TABLE tbl_order_line ADD INDEX idx_order_line_item_id (item_id), ALGORITHM=INPLACE, LOCK=NONE;
