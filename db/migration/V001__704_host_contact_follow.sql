-- V001__704_host_contact_follow.sql
-- 이슈: Gosrock/DuDoong-Backend#704
-- 목적: v2 호스트 — 대표 연락처 N개(tbl_host_contact), 관심 호스트(tbl_host_follow), 커버 이미지(tbl_host.cover_image_key)
-- v1 영향: 없음 (테이블 추가 + nullable 컬럼 추가)
--   - 기존 호스트의 연락처는 이관하지 않는다. 연락처 테이블이 비어 있으면 v2 조회 시 tbl_host.contact_email / contact_number 로 대체 표시
--   - v2 로 연락처 저장 시 첫 EMAIL / PHONE 을 tbl_host.contact_email / contact_number 에도 기록 (v1 화면 호환)
-- 실행 기록:
--   staging: YYYY-MM-DD (실행자)
--   prod:    YYYY-MM-DD (실행자)

CREATE TABLE `tbl_host_contact` (
  `host_contact_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `host_id` bigint NOT NULL,
  `type` varchar(20) NOT NULL,
  `contact_value` varchar(200) NOT NULL,
  `sort_order` int NOT NULL,
  PRIMARY KEY (`host_contact_id`),
  KEY `idx_host_contact_host_id` (`host_id`),
  CONSTRAINT `fk_host_contact_host` FOREIGN KEY (`host_id`) REFERENCES `tbl_host` (`host_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tbl_host_follow` (
  `host_follow_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `host_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`host_follow_id`),
  UNIQUE KEY `uk_host_follow_host_user` (`host_id`, `user_id`),
  KEY `idx_host_follow_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `tbl_host`
  ADD COLUMN `cover_image_key` varchar(255) DEFAULT NULL;
