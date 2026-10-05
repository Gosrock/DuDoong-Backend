-- V010__740_event_place_detail.sql
-- 이슈: Gosrock/DuDoong-Backend#740
-- 목적: v2 공연 기본 정보(E-4) 장소 '상세주소' (Figma 424:9918 "상세주소 추가", 전수조사 A10-07)
--   - tbl_event.place_detail_address: 선택 입력, 최대 255자(기존 place_address 와 같은 길이). 기존 공연은 NULL
--   - v1·운영 어드민 장소 수정은 이 값을 모른다 → 주소가 그대로면 유지, 바뀌면 지움 (EventPlace.keepingDetailOf)
-- v1 영향: 없음 (nullable 컬럼 추가, v1 응답에는 넣지 않음)
-- 실행 순서: (1) 이 파일 DDL → (2) 앱 배포 (새 앱이 컬럼을 매핑하므로 DDL 먼저). [DATA] 없음
-- 실행 기록:
--   staging: YYYY-MM-DD (실행자)
--   prod:    YYYY-MM-DD (실행자)

SET NAMES utf8mb4;

ALTER TABLE tbl_event ADD COLUMN place_detail_address VARCHAR(255) NULL, ALGORITHM=INSTANT;
