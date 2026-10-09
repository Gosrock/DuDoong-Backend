-- V011__763_admin_audit_log.sql
-- 이슈: Gosrock/DuDoong-Backend#763
-- 목적: 운영 어드민(/internal-api) 감사 기록 — 상태 변경 요청(POST·PUT·PATCH·DELETE)과 엑셀 반출(export)마다 1행
--   - 누가(actor_user_id) · 무엇을(action = 컨트롤러.메서드, http_method, request_path) · 대상(target = 경로 변수 JSON)
--   - 요청 내용(request_detail = 본문 JSON 또는 엑셀 필터 파라미터 JSON), 대상의 변경 전후 핵심 값(before_value·after_value JSON)
--   - 결과(result = SUCCESS / FAIL, error_code)
--   - 긴 값은 앱에서 2000자로 자른다. 쓰기만 하고 고치지 않는다
--   - 인덱스
--     idx_admin_audit_log_actor_user_id (actor_user_id, admin_audit_log_id): 관리자별 최근 기록
--     idx_admin_audit_log_created_at (created_at): 기간 조회
-- v1 영향: 없음 (테이블 추가)
-- 실행 순서: (1) 이 파일 DDL → (2) 앱 배포. [DATA] 없음
--   - DDL 없이 새 앱이 뜨면 어드민 요청은 그대로 처리되고 감사 기록 저장만 실패한다(오류 로그 + 구조화 로그는 남음)
-- 실행 기록:
--   staging: YYYY-MM-DD (실행자)
--   prod:    YYYY-MM-DD (실행자)

SET NAMES utf8mb4;

CREATE TABLE `tbl_admin_audit_log` (
  `admin_audit_log_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `actor_user_id` bigint NOT NULL,
  `action` varchar(100) NOT NULL,
  `http_method` varchar(10) NOT NULL,
  `request_path` varchar(255) NOT NULL,
  `target` varchar(255) DEFAULT NULL,
  `request_detail` varchar(2000) DEFAULT NULL,
  `before_value` varchar(2000) DEFAULT NULL,
  `after_value` varchar(2000) DEFAULT NULL,
  `result` varchar(20) NOT NULL,
  `error_code` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`admin_audit_log_id`),
  KEY `idx_admin_audit_log_actor_user_id` (`actor_user_id`, `admin_audit_log_id`),
  KEY `idx_admin_audit_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
