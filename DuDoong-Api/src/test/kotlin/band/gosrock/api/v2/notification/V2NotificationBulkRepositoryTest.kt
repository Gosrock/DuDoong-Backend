package band.gosrock.api.v2.notification

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.domain.domains.notification.domain.Notification
import band.gosrock.domain.domains.notification.domain.NotificationTargetType
import band.gosrock.domain.domains.notification.domain.NotificationType
import band.gosrock.domain.domains.notification.repository.NotificationBulkRepository
import band.gosrock.domain.domains.notification.repository.NotificationRepository
import java.util.concurrent.atomic.AtomicLong
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

/** 알림 일괄 저장 (#714): 사전 조회로 거르는 경로, 사전 조회 뒤 경합으로 uk 에 걸리는 폴백(행 단위 재시도) 경로, null event_id */
@ApiIntegrateSpringBootTest
@DisplayName("v2 알림센터 - 일괄 저장")
class V2NotificationBulkRepositoryTest {

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var bulkRepository: NotificationBulkRepository

    @Autowired private lateinit var notificationRepository: NotificationRepository

    /** 사전 조회가 항상 "없음" 을 돌려줘, 그 사이 다른 처리가 먼저 넣은 상황(uk 위반)을 만든다 */
    private val racingRepository by lazy {
        object : NotificationBulkRepository(jdbcTemplate) {
            override fun existingKeys(notifications: List<Notification>): Set<Triple<Long, String, String>> = emptySet()
        }
    }

    private fun n(userId: Long, key: String, type: NotificationType = NotificationType.ORDER_APPROVED, eventId: Long? = 1L) = Notification(
        userId = userId,
        type = type,
        title = "제목",
        body = "본문",
        targetType = if (eventId == null) NotificationTargetType.HOST else NotificationTargetType.ORDER,
        targetId = key,
        eventId = eventId,
        extra = null,
        dedupKey = key,
    )

    private fun rows(userId: Long) = notificationRepository.findAllByUserId(userId)

    @Test
    fun `사전 조회 - 이미 있는 키와 같은 요청 안의 중복은 건너뛰고 나머지만 한 문장으로 저장`() {
        val user = nextUserId()
        assertEquals(1, bulkRepository.insertSkippingDuplicates(listOf(n(user, "a"))))
        assertEquals(1, bulkRepository.insertSkippingDuplicates(listOf(n(user, "a"), n(user, "b"), n(user, "b"))))
        assertEquals(0, bulkRepository.insertSkippingDuplicates(listOf(n(user, "a"), n(user, "b"))))
        assertEquals(listOf("a", "b"), rows(user).map { it.dedupKey }.sorted())
        // 같은 키라도 종류가 다르면 다른 알림
        assertEquals(1, bulkRepository.insertSkippingDuplicates(listOf(n(user, "a", NotificationType.ORDER_REFUSED))))
    }

    @Test
    fun `폴백 - 사전 조회를 통과한 뒤 uk 에 걸리면 행 단위로 다시 넣고 중복 행만 버린다`() {
        val user = nextUserId()
        bulkRepository.insertSkippingDuplicates(listOf(n(user, "dup")))

        assertEquals(2, racingRepository.insertSkippingDuplicates(listOf(n(user, "new1"), n(user, "dup"), n(user, "new2"))))
        assertEquals(listOf("dup", "new1", "new2"), rows(user).map { it.dedupKey }.sorted())
        // 전부 중복 → 0건, 예외 없음
        assertEquals(0, racingRepository.insertSkippingDuplicates(listOf(n(user, "dup"), n(user, "new1"))))
        assertEquals(3, rows(user).size)
    }

    @Test
    fun `HOST 알림은 event_id null 로 저장`() {
        val user = nextUserId()
        bulkRepository.insertSkippingDuplicates(listOf(n(user, "host_user:1", NotificationType.HOST_MEMBER_ADDED, eventId = null)))
        assertNull(rows(user).single().eventId)
    }

    companion object {
        /** 다른 테스트의 유저 id 와 겹치지 않는 수신자 id (FK 없음) */
        private val ids = AtomicLong(9_000_000_000L)
        private fun nextUserId() = ids.incrementAndGet()
    }
}
