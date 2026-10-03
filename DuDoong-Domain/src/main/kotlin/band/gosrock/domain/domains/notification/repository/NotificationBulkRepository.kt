package band.gosrock.domain.domains.notification.repository

import band.gosrock.domain.domains.notification.domain.Notification
import java.sql.Timestamp
import java.time.LocalDateTime
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

/**
 * 알림 일괄 저장. IDENTITY 키라 JPA 는 insert 를 묶지 못하므로 여러 행을 한 문장(multi-row INSERT)으로 넣는다.
 * 중복(uk(user_id, type, dedup_key) — 같은 알림 재처리)은 건너뛴다: 먼저 이미 있는 키를 걸러 내고, 그 사이 동시 처리로 uk 에 걸리면
 * 행 단위로 다시 넣으며 중복 행만 버린다. (`INSERT IGNORE` 는 테스트 DB(H2 2.x)가 지원하지 않고, 길이 초과 등 다른 오류까지 경고로 삼켜서 쓰지 않는다)
 * 길이는 호출자가 컬럼 길이 안으로 자른다.
 */
@Repository
class NotificationBulkRepository(private val jdbcTemplate: JdbcTemplate) {

    /** @return 실제 저장된 행 수 (중복으로 건너뛴 행 제외) */
    fun insertSkippingDuplicates(notifications: List<Notification>, now: LocalDateTime = LocalDateTime.now()): Int {
        val existing = existingKeys(notifications)
        val fresh = notifications.distinctBy { it.key() }.filterNot { it.key() in existing }
        if (fresh.isEmpty()) return 0
        return try {
            insert(fresh, now)
        } catch (e: DuplicateKeyException) {
            fresh.sumOf {
                try {
                    insert(listOf(it), now)
                } catch (duplicated: DuplicateKeyException) {
                    0
                }
            }
        }
    }

    private fun existingKeys(notifications: List<Notification>): Set<Triple<Long, String, String>> {
        if (notifications.isEmpty()) return emptySet()
        val userIds = notifications.map { it.userId }.distinct()
        val dedupKeys = notifications.map { it.dedupKey }.distinct()
        val sql = "SELECT user_id, type, dedup_key FROM tbl_notification WHERE user_id IN (${placeholders(userIds.size)}) " +
            "AND dedup_key IN (${placeholders(dedupKeys.size)})"
        return jdbcTemplate.query(sql, { rs, _ -> Triple(rs.getLong(1), rs.getString(2), rs.getString(3)) }, *(userIds + dedupKeys).toTypedArray())
            .toSet()
    }

    private fun insert(notifications: List<Notification>, now: LocalDateTime): Int {
        val row = "(${placeholders(COLUMNS.size)})"
        val sql = "INSERT INTO tbl_notification (${COLUMNS.joinToString(", ")}) VALUES " + notifications.joinToString(", ") { row }
        val at = Timestamp.valueOf(now)
        val args = notifications.flatMap {
            listOf(it.userId, it.type.name, it.title, it.body, it.targetType.name, it.targetId, it.eventId, it.extra, it.dedupKey, false, at, at)
        }
        return jdbcTemplate.update(sql, *args.toTypedArray())
    }

    private fun Notification.key() = Triple(userId, type.name, dedupKey)

    private fun placeholders(n: Int) = List(n) { "?" }.joinToString(", ")

    companion object {
        private val COLUMNS = listOf(
            "user_id", "type", "title", "body", "target_type", "target_id", "event_id", "extra", "dedup_key", "is_read", "created_at", "updated_at",
        )
    }
}
