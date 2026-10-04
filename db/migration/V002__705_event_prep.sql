-- V002__705_event_prep.sql
-- 이슈: Gosrock/DuDoong-Backend#705
-- 목적: v2 공연 준비 — 종료 시각(tbl_event.end_at), 티켓 여부(tbl_event.has_ticket), 공연 문의처 N개(tbl_event_contact),
--       상세 정보 섹션(tbl_event_section), 태그(tbl_tag) + 공연-태그(tbl_event_tag)
-- v1 영향: 없음 (nullable 컬럼 + 기본값 있는 NOT NULL 컬럼 + 테이블 추가)
--   - end_at: 쓰기 전용 비정규화 컬럼. 앱은 v1/v2 어느 경로로 저장해도 start_at + run_time(분) 으로 기록하고,
--             v1 이 살아있는 동안 조회·정산·종료 배치의 기준값은 계산값(start_at + run_time)이다
--   - has_ticket: 기존 공연은 1(티켓 있음). v1 은 이 컬럼을 읽지 않는다 (v1 오픈은 항상 티켓 필요)
--   - 첫 섹션(sort_order 최소)이 '공연 소개' 이고 본문이 tbl_event.content 와 동기화된다. 섹션이 없는 공연은 앱이 content 를 대체 표시
--   - content_format: v2 저장 = HTML(서버 sanitize), v1 에서 온 본문(이관 / v1 수정 동기화) = MARKDOWN
--   - 문의처는 v1 대응 컬럼이 없어 동기화하지 않는다
-- 실행 순서: (1) 이 파일의 DDL(-- [DATA] 위) → (2) 앱 배포 → (3) -- [DATA] 블록
--   - DDL 은 컬럼/테이블 추가만이라 기존 v1 앱에 영향 없음. 새 앱은 새 컬럼이 없으면 기동 후 tbl_event 조회가 실패하므로 DDL 이 먼저
--   - [DATA] 는 앱 배포 후 실행: 배포 중 옛 앱이 쓴 content / run_time 까지 반영하고, 재실행해도 안전(멱등)
--   - 실행 후 확인: SELECT HEX(title) FROM tbl_event_section WHERE sort_order = 0 LIMIT 1;  → 'EAB3B5EC97B020EC868CEAB09C' (= '공연 소개' utf8mb4)
-- 실행 기록:
--   staging: YYYY-MM-DD (실행자)
--   prod:    YYYY-MM-DD (실행자)

SET NAMES utf8mb4;

ALTER TABLE `tbl_event`
  ADD COLUMN `end_at` datetime DEFAULT NULL,
  ADD COLUMN `has_ticket` bit(1) NOT NULL DEFAULT b'1',
  ALGORITHM=INSTANT;

CREATE TABLE `tbl_event_contact` (
  `event_contact_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `event_id` bigint NOT NULL,
  `type` varchar(20) NOT NULL,
  `contact_value` varchar(200) NOT NULL,
  `sort_order` int NOT NULL,
  PRIMARY KEY (`event_contact_id`),
  KEY `idx_event_contact_event_id` (`event_id`),
  CONSTRAINT `fk_event_contact_event` FOREIGN KEY (`event_id`) REFERENCES `tbl_event` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tbl_event_section` (
  `event_section_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `event_id` bigint NOT NULL,
  `title` varchar(20) NOT NULL,
  `content` longtext,
  `content_format` varchar(20) NOT NULL DEFAULT 'HTML',
  `sort_order` int NOT NULL,
  PRIMARY KEY (`event_section_id`),
  KEY `idx_event_section_event_id` (`event_id`),
  CONSTRAINT `fk_event_section_event` FOREIGN KEY (`event_id`) REFERENCES `tbl_event` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tbl_tag` (
  `tag_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `category` varchar(20) NOT NULL,
  `name` varchar(30) NOT NULL,
  `sort_order` int NOT NULL,
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `uk_tag_category_name` (`category`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tbl_event_tag` (
  `event_tag_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `event_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  PRIMARY KEY (`event_tag_id`),
  UNIQUE KEY `uk_event_tag_event_tag` (`event_id`, `tag_id`),
  KEY `idx_event_tag_tag_id` (`tag_id`),
  CONSTRAINT `fk_event_tag_event` FOREIGN KEY (`event_id`) REFERENCES `tbl_event` (`event_id`),
  CONSTRAINT `fk_event_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tbl_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- [DATA] 아래는 앱 배포 후 실행한다. 재실행해도 안전하다 (멱등)
-- [DATA] 블록만 따로 실행할 때도 문자셋을 먼저 맞춘다
SET NAMES utf8mb4;

-- [DATA] 1. 초기 태그 (DEC-014). unique(category, name) 이라 이미 있으면 건너뛴다
INSERT INTO `tbl_tag` (`created_at`, `updated_at`, `category`, `name`, `sort_order`) VALUES
  (NOW(), NOW(), 'EVENT_TYPE', '정기공연', 0),
  (NOW(), NOW(), 'EVENT_TYPE', '연합공연', 1),
  (NOW(), NOW(), 'EVENT_TYPE', '시리즈공연', 2),
  (NOW(), NOW(), 'EVENT_TYPE', '단독공연', 3),
  (NOW(), NOW(), 'EVENT_TYPE', '페스티벌', 4),
  (NOW(), NOW(), 'EVENT_TYPE', '쇼케이스', 5),
  (NOW(), NOW(), 'EVENT_TYPE', '연주회', 6),
  (NOW(), NOW(), 'GENRE', '락밴드', 0),
  (NOW(), NOW(), 'GENRE', '어쿠스틱·포크', 1),
  (NOW(), NOW(), 'GENRE', '클래식', 2),
  (NOW(), NOW(), 'GENRE', 'K-pop', 3),
  (NOW(), NOW(), 'GENRE', 'J-pop', 4),
  (NOW(), NOW(), 'GENRE', '힙합', 5),
  (NOW(), NOW(), 'GENRE', '메탈', 6),
  (NOW(), NOW(), 'AREA', '홍대', 0),
  (NOW(), NOW(), 'AREA', '합정', 1),
  (NOW(), NOW(), 'AREA', '신촌', 2),
  (NOW(), NOW(), 'AREA', '강남', 3),
  (NOW(), NOW(), 'TEAM', '대학밴드', 0),
  (NOW(), NOW(), 'TEAM', '직장인밴드', 1),
  (NOW(), NOW(), 'TEAM', '실용음악과', 2),
  (NOW(), NOW(), 'TEAM', '인디', 3)
ON DUPLICATE KEY UPDATE `tag_id` = `tag_id`;

-- [DATA] 2. end_at 을 기준값(start_at + run_time) 으로 채운다. 비어 있거나 어긋난(stale) 행만 고친다
UPDATE `tbl_event`
SET `end_at` = TIMESTAMPADD(MINUTE, `run_time`, `start_at`)
WHERE `start_at` IS NOT NULL AND `run_time` IS NOT NULL
  AND NOT (`end_at` <=> TIMESTAMPADD(MINUTE, `run_time`, `start_at`));

-- [DATA] 3. 섹션이 없는 기존 공연의 content(빈 값 아님)를 첫 섹션(sort_order 0, '공연 소개', MARKDOWN)으로 이관 (삭제 공연 제외).
--           앱도 섹션이 없으면 content 를 대체 표시하므로 이관은 선택이지만, 이관해 두면 이후 v2 저장과 형태가 같아진다
INSERT INTO `tbl_event_section` (`created_at`, `updated_at`, `event_id`, `title`, `content`, `content_format`, `sort_order`)
SELECT NOW(), NOW(), e.`event_id`, '공연 소개', e.`content`, 'MARKDOWN', 0
FROM `tbl_event` e
WHERE e.`content` IS NOT NULL
  AND TRIM(e.`content`) <> ''
  AND (e.`status` IS NULL OR e.`status` <> 'DELETED')
  AND NOT EXISTS (SELECT 1 FROM `tbl_event_section` s WHERE s.`event_id` = e.`event_id`);
