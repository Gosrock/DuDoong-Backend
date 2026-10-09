package band.gosrock.domain.domains.audit.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

/**
 * 운영 어드민 감사 기록의 변경 전후 핵심 값 (#763). 대상 종류별로 몇 개 컬럼만 읽는다.
 * JPA 가 아니라 JDBC 로 읽는 이유: 요청 EntityManager(open-in-view)에 대상 엔티티를 먼저 올리면
 * 유스케이스의 잠금 조회가 그 엔티티를 그대로 돌려줘 최신 값을 못 본다 ([CartLockKeyQuery] 와 같은 기준).
 * 개인정보(연락처·계좌)는 읽지 않는다.
 */
@Repository
class AdminAuditSnapshotQuery(private val jdbcTemplate: JdbcTemplate) {

    fun user(userId: Long): Map<String, Any?>? =
        one("SELECT account_role, account_state, name FROM tbl_user WHERE user_id = ?", userId)

    fun event(eventId: Long): Map<String, Any?>? =
        one("SELECT status, name, start_at FROM tbl_event WHERE event_id = ?", eventId)

    fun host(hostId: Long): Map<String, Any?>? =
        one("SELECT name, partner, master_user_id FROM tbl_host WHERE host_id = ?", hostId)

    fun hostMember(hostId: Long, userId: Long): Map<String, Any?>? =
        one("SELECT role, active FROM tbl_host_user WHERE host_id = ? AND user_id = ?", hostId, userId)

    fun order(orderUuid: String): Map<String, Any?>? =
        one("SELECT order_status, refund_status FROM tbl_order WHERE uuid = ?", orderUuid)

    fun ticketItem(ticketItemId: Long): Map<String, Any?>? =
        one(
            "SELECT name, quantity, supply_count, purchase_limit, is_sellable, ticket_item_status FROM tbl_ticket_item WHERE ticket_item_id = ?",
            ticketItemId,
        )

    fun comment(commentId: Long): Map<String, Any?>? =
        one("SELECT comment_status FROM tbl_comment WHERE comment_id = ?", commentId)

    // 컬럼 이름은 소문자로 맞춘다 (H2 는 대문자로 돌려준다)
    private fun one(sql: String, vararg args: Any): Map<String, Any?>? =
        jdbcTemplate.queryForList(sql, *args).firstOrNull()?.mapKeys { it.key.lowercase() }
}
