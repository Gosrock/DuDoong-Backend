-- V009__729_mypage.sql
-- 이슈: Gosrock/DuDoong-Backend#729
-- 목적: v2 마이페이지 관심 호스트(M-4) — tbl_event (host_id, status) 인덱스
--   - 페이지 호스트들의 공개 공연: WHERE host_id IN (...) AND status IN ('OPEN','CALCULATING','CLOSED')
--   - '전체 상태' 필터 EXISTS: host_id = ? AND status = 'OPEN' AND 종료 식 > now (ACTIVE) / host_id = ? AND status IN (공개) (ENDED)
--   기존 tbl_event 인덱스는 PK, uk_event_check_in_token(check_in_token), idx_event_status_start_at(status, start_at) 뿐이라 host_id 조건을 못 쓴다 (baseline·V004·V006 확인)
--   EXPLAIN (로컬 MySQL 8.4, prod 분포의 합성 데이터: 공연 1,835 = OPEN 32 / PREPARING 241 / CLOSED 1,080 / DELETED 482, 호스트 400, 팔로우 20, 2026-10-05)
--     공개 공연 IN 조회: 인덱스 없음 type=ALL 1,835행 → range idx_event_host_id_status 94행
--     ENDED 필터의 공개 공연 EXISTS: MATERIALIZED type=ALL 1,835행 → ref(host_id) 4행 Using index
--     ACTIVE EXISTS: ref(status='OPEN') 32행 → ref(host_id, status) 1행
--   호스트 공연 리스트(H-14)·내 공연(E-1) 등 기존 host_id 조건 조회도 이 인덱스를 쓸 수 있다
-- v1 영향: 없음 (인덱스 추가만, INPLACE·LOCK=NONE)
-- 실행 순서: 앱 배포 전후 무관 (없어도 동작, 느릴 뿐). [DATA] 없음
-- 실행 기록:
--   staging: YYYY-MM-DD (실행자)
--   prod:    YYYY-MM-DD (실행자)

SET NAMES utf8mb4;

ALTER TABLE tbl_event ADD INDEX idx_event_host_id_status (host_id, status), ALGORITHM=INPLACE, LOCK=NONE;
