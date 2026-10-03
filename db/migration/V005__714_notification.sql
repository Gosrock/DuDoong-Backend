-- V005__714_notification.sql
-- 이슈: Gosrock/DuDoong-Backend#714
-- 목적: v2 알림센터(최소 범위) — 알림 저장 테이블 tbl_notification
--   - 저장: 기존 도메인 이벤트(CreateOrder·DoneOrder·WithDrawOrder)와 v2 멤버 추가 이벤트에 붙은 저장 핸들러가 커밋 후 비동기로 넣는다
--     (v1 경로의 승인형 주문 생성·승인·거절도 저장). 종류: HOST_MEMBER_ADDED / ORDER_PENDING_APPROVE / ORDER_APPROVED / ORDER_REFUSED
--   - target_type + target_id: 딥링크 대상 (HOST = hostId, ORDER = orderUuid). event_id: ORDER 대상의 공연 id
--   - extra: 부가 정보 JSON 문자열 (호스트명, 역할, 공연명, 주문 번호, 거절 사유 종류·문구)
--   - dedup_key: 중복 방지 키. 주문 알림 = orderUuid, 멤버 추가 = host_user:{host_user_id}
--   - uk(user_id, type, dedup_key): 같은 이벤트 재처리 시 같은 사람에게 두 번 저장하지 않음
--   - idx(user_id, is_read): 안읽음 수(N-2), 전체 읽음(N-3)
--   - idx(user_id): 목록 최신순(N-1, WHERE user_id = ? ORDER BY notification_id DESC — 보조 인덱스에 PK 가 붙어 정렬 없이 읽음)
--   - 보관 기간·삭제 정책은 후속 (행 증가 추정: prod 승인형 주문 월 약 1,500건 × (마스터·매니저 평균 1.4명 + 주문자 1) → 월 수천 행)
-- v1 영향: 없음 (테이블 추가만. v1 앱은 이 테이블을 읽지도 쓰지도 않는다)
-- 실행 순서: (1) 이 파일 DDL → (2) 앱 배포. [DATA] 없음 (기존 주문·멤버에 대한 알림은 소급 생성하지 않는다)
--   - 새 앱은 테이블이 없으면 알림 저장(비동기, 실패는 로그만)과 N-1~N-3 이 실패하므로 DDL 이 먼저
-- 실행 기록:
--   staging: YYYY-MM-DD (실행자)
--   prod:    YYYY-MM-DD (실행자)

SET NAMES utf8mb4;

CREATE TABLE `tbl_notification` (
  `notification_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `type` varchar(40) NOT NULL,
  `title` varchar(100) NOT NULL,
  `body` varchar(500) NOT NULL,
  `target_type` varchar(20) NOT NULL,
  `target_id` varchar(64) NOT NULL,
  `event_id` bigint DEFAULT NULL,
  `extra` varchar(1000) DEFAULT NULL,
  `dedup_key` varchar(100) NOT NULL,
  `is_read` bit(1) NOT NULL DEFAULT b'0',
  `read_at` datetime DEFAULT NULL,
  PRIMARY KEY (`notification_id`),
  UNIQUE KEY `uk_notification_user_type_dedup` (`user_id`, `type`, `dedup_key`),
  KEY `idx_notification_user_id_is_read` (`user_id`, `is_read`),
  KEY `idx_notification_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
