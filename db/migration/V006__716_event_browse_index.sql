-- V006__716_event_browse_index.sql
-- 이슈: Gosrock/DuDoong-Backend#716
-- 목적: v2 사용자 앱 공연 탐색 — tbl_event (status, start_at) 인덱스
--   - P-1 홈 캐러셀 / P-2 includePast=false: 종료 전 등록 공연 WHERE status = 'OPEN' AND TIMESTAMPADD(MINUTE, COALESCE(run_time, 0), start_at) > now
--       (종료 시각 = startAt + runTime, 종료 배치와 같은 식. 진행 중 공연 포함 — #716 M-1)
--       인덱스 없음: type=ALL 전체 + filesort → 인덱스: ref(status='OPEN') 32행 후 종료 식 필터. P-1 은 (status, start_at) 순서 그대로라 filesort 없음,
--       P-2 는 정렬식 CASE 라 filesort(32행). 건수 쿼리도 ref 32행
--       (로컬 MySQL 8.4, prod 상태 분포 그대로 OPEN 32 / PREPARING 241 / CLOSED 1,080 / DELETED 482, run_time 최대 30,000분 행 포함, 2026-10-04)
--     start_at 하한 보조 조건(now - 최대 runTime)은 두지 않았다: runTime 상한이 없어(prod 최대 30,000분) 고정 상한이면 장기 공연이 빠질 수 있고,
--     OPEN 행이 수십 건이라 status 동등 조건만으로 충분
--   - P-2 includePast=true 는 공개 상태가 대부분이라 그대로 스캔(~940행). 태그 필터는 기존 idx_event_tag_tag_id / uk_event_tag_event_tag 로 semi-join, 호스트명 검색은 tbl_host PK eq_ref
-- v1 영향: 없음 (인덱스 추가만, INPLACE·LOCK=NONE)
-- 실행 순서: 앱 배포 전후 무관 (없어도 동작, 느릴 뿐). [DATA] 없음
-- 실행 기록:
--   staging: YYYY-MM-DD (실행자)
--   prod:    YYYY-MM-DD (실행자)

SET NAMES utf8mb4;

ALTER TABLE tbl_event ADD INDEX idx_event_status_start_at (status, start_at), ALGORITHM=INPLACE, LOCK=NONE;
