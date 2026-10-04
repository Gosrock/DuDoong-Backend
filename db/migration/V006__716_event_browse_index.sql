-- V006__716_event_browse_index.sql
-- 이슈: Gosrock/DuDoong-Backend#716
-- 목적: v2 사용자 앱 공연 탐색 — tbl_event (status, start_at) 인덱스
--   - P-1 홈 캐러셀 WHERE status = 'OPEN' AND start_at > now ORDER BY start_at LIMIT 10
--       인덱스 없음: type=ALL 1,000행 + filesort → 인덱스: range(40행), filesort 없음 (로컬 MySQL 8.4, 공연 1,000 / OPEN 40, 2026-10-04)
--   - P-2 공연 리스트 includePast=false(status = 'OPEN' AND start_at > now): ALL 1,000행 → range 35행 (+ 정렬식 CASE 라 filesort, 35행), 건수 쿼리는 Using index
--   - P-2 includePast=true 는 공개 상태가 대부분이라 그대로 스캔(~940행). 태그 필터는 기존 idx_event_tag_tag_id / uk_event_tag_event_tag 로 semi-join, 호스트명 검색은 tbl_host PK eq_ref
-- v1 영향: 없음 (인덱스 추가만, INPLACE·LOCK=NONE)
-- 실행 순서: 앱 배포 전후 무관 (없어도 동작, 느릴 뿐). [DATA] 없음
-- 실행 기록:
--   staging: YYYY-MM-DD (실행자)
--   prod:    YYYY-MM-DD (실행자)

SET NAMES utf8mb4;

ALTER TABLE tbl_event ADD INDEX idx_event_status_start_at (status, start_at), ALGORITHM=INPLACE, LOCK=NONE;
