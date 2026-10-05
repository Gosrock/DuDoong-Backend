-- V008__719_ticket_gift.sql
-- 이슈: Gosrock/DuDoong-Backend#719
-- 목적: v2 사용자 앱 8단계 티켓탭·선물 (11 문서 8장, DEC-022·DEC-026)
--   - tbl_ticket_gift: 티켓 선물 링크 (받는 사람을 미리 고르지 않는 링크 방식). 한 티켓에 기록이 여러 개일 수 있다(반환·거절·취소 뒤 재선물)
--     - status: PENDING / ACCEPTED / REJECTED / RETURNED / CANCELED (시간 만료 상태 없음 — 공연 종료 뒤 PENDING 은 조회 시 '선물 만료', DEC-026 #7)
--     - cancel_reason: SENDER(회수) / ORDER_CANCELED(주문 취소 연쇄) / SENDER_WITHDRAWN(보낸 사람 탈퇴·정지) / EVENT_REMOVED(공연 운영 삭제·비공개)
--     - token: 추측 불가 랜덤(32바이트 SecureRandom, base64url 43자). 요청 로그·Slack 의 URL 에서는 가린다
--       base64url 은 대소문자를 구분하므로 utf8mb4_bin (기본 _ai_ci 면 대소문자만 다른 토큰이 같은 값으로 조회·uk 충돌)
--     - receiver_user_id: 수락·거절한 사람. memo: 보낸 사람 메모(최대 50자, 보낸 사람에게만 보임)
--     - order_uuid·event_id: 티켓에서 복사 (바뀌지 않음). 주문·공연 단위 연쇄 처리와 잠금 키 조회용
--   - 인덱스
--     uk_ticket_gift_token: 랜딩·수락·거절(G-3·G-4·G-5) 단건 조회 + 토큰 중복 방지
--     idx_ticket_gift_issued_ticket_id (issued_ticket_id, ticket_gift_id): 티켓의 가장 최근 선물(선물 상태 판정, 대기 여부 — v1 입장·상세·환불 보호 포함)
--     idx_ticket_gift_sender_user_id (sender_user_id, ticket_gift_id): G-7 보낸 선물(최신 순), T-1 '선물 완료' 행, 보낸 사람 탈퇴 연쇄
--     idx_ticket_gift_receiver_user_id (receiver_user_id, ticket_gift_id): G-7 받은 선물(최신 순)
--     idx_ticket_gift_event_id_status (event_id, status): 공연 운영 삭제 연쇄 (대기 선물)
--     idx_ticket_gift_order_uuid_status (order_uuid, status): 주문 연쇄(주문 취소·환불·거절마다 실행) — 대기 선물을 먼저 찾고 없으면 끝낸다.
--       선물이 없는 주문(대부분)은 이 인덱스 조회 1회뿐이고 티켓 행을 잠그지 않는다
--   - tbl_issued_ticket.idx_issued_ticket_user_id_id (user_id, issued_ticket_id): T-1 내 티켓(현재 소유분). 기존 인덱스는 (event_id, user_id) 라 user_id 단독 조회는 전체 스캔
--   - 선물 QR 은 기존 tbl_issued_ticket.uuid 를 수락·반환 때 새 값으로 바꾼다 (DEC-026 #6) → tbl_issued_ticket 컬럼 변경 없음
-- v1 영향: 없음 (테이블·인덱스 추가만). 선물이 없으면 v1 의 모든 판정이 기존과 같다 (prod 2026-10-05: 발급 티켓 73,139장 모두 소유자 = 주문자)
-- 실행 순서: (1) 이 파일 DDL → (2) 앱 배포. [DATA] 없음
--   - 새 앱은 v1 입장·환불·티켓 상세에서도 tbl_ticket_gift 를 읽으므로 DDL 이 없으면 v1 경로까지 실패한다 → 반드시 DDL 먼저
-- 실행 기록:
--   staging: YYYY-MM-DD (실행자)
--   prod:    YYYY-MM-DD (실행자)

SET NAMES utf8mb4;

CREATE TABLE `tbl_ticket_gift` (
  `ticket_gift_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `issued_ticket_id` bigint NOT NULL,
  `order_uuid` varchar(64) NOT NULL,
  `event_id` bigint NOT NULL,
  `sender_user_id` bigint NOT NULL,
  `receiver_user_id` bigint DEFAULT NULL,
  `token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `status` varchar(20) NOT NULL,
  `cancel_reason` varchar(30) DEFAULT NULL,
  `memo` varchar(50) DEFAULT NULL,
  `accepted_at` datetime DEFAULT NULL,
  `rejected_at` datetime DEFAULT NULL,
  `returned_at` datetime DEFAULT NULL,
  `canceled_at` datetime DEFAULT NULL,
  PRIMARY KEY (`ticket_gift_id`),
  UNIQUE KEY `uk_ticket_gift_token` (`token`),
  KEY `idx_ticket_gift_issued_ticket_id` (`issued_ticket_id`, `ticket_gift_id`),
  KEY `idx_ticket_gift_sender_user_id` (`sender_user_id`, `ticket_gift_id`),
  KEY `idx_ticket_gift_receiver_user_id` (`receiver_user_id`, `ticket_gift_id`),
  KEY `idx_ticket_gift_event_id_status` (`event_id`, `status`),
  KEY `idx_ticket_gift_order_uuid_status` (`order_uuid`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE tbl_issued_ticket ADD INDEX idx_issued_ticket_user_id_id (user_id, issued_ticket_id), ALGORITHM=INPLACE, LOCK=NONE;
