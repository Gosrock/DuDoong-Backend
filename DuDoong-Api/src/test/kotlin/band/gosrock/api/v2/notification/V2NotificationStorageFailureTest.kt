package band.gosrock.api.v2.notification

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.operation.V2OperationTestSupport
import band.gosrock.domain.domains.host.repository.HostRepository
import band.gosrock.domain.domains.notification.domain.Notification
import band.gosrock.domain.domains.notification.domain.NotificationType
import band.gosrock.domain.domains.notification.repository.NotificationBulkRepository
import band.gosrock.domain.domains.notification.repository.NotificationRepository
import band.gosrock.domain.domains.order.domain.OrderStatus
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.jdbc.core.JdbcTemplate

/**
 * 실제 저장 실패 격리 (#714): 알림 일괄 저장(`NotificationBulkRepository`)만 DB 오류(DataAccessException)를 던지게 하고,
 * 서비스·핸들러·트랜잭션은 실제 빈 그대로 둔다. 원 요청은 성공하고 데이터가 남으며, 오류가 풀린 뒤에는 알림이 정상 저장된다.
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@Import(V2NotificationStorageFailureTest.FailingBulkRepositoryConfig::class)
@DisplayName("v2 알림센터 - 저장 실패(DB 오류) 격리")
class V2NotificationStorageFailureTest : V2OperationTestSupport() {

    @Autowired private lateinit var hostRepository: HostRepository

    @Autowired private lateinit var notificationRepository: NotificationRepository

    private fun awaitAttempts(count: Int) {
        val deadline = System.currentTimeMillis() + 10_000
        while (ATTEMPTS.get() < count && System.currentTimeMillis() < deadline) Thread.sleep(50)
        assertTrue(ATTEMPTS.get() >= count, "저장 시도 ${count}회 기대, 실제 ${ATTEMPTS.get()}")
    }

    @Test
    fun `저장 DB 오류 - 멤버 추가, v1 주문 생성·승인, v2 거절 성공하고 데이터 유지, 오류가 풀리면 정상 저장`() {
        FAIL.set(true)
        ATTEMPTS.set(0)

        val shop = Shop() // 멤버 추가 → 1회
        awaitAttempts(1)
        val host = hostRepository.findById(shop.team.hostId).get()
        assertTrue(host.isActiveHostUserId(shop.team.manager.id!!) && host.isActiveHostUserId(shop.team.guest.id!!))

        val buyer = newBuyer()
        // 승인 대기 알림은 핸들러가 돌 때 이미 승인됐으면 저장 시도 없이 끝날 수 있어, 반드시 시도되는 것(멤버 추가·승인·거절)만 센다
        val approved = shop.approved(buyer)
        awaitAttempts(2)
        assertEquals(OrderStatus.APPROVED, orderRepository.findByUuidIn(listOf(approved)).single().orderStatus)
        assertEquals(1, issuedTicketRepository.findAllByOrderUuid(approved).size)

        val refused = shop.order(newBuyer())
        refuse(shop.team.manager, shop.eventId, refused, "SOLD_OUT").andExpect { status { isOk() } }
        awaitAttempts(3)
        assertEquals(OrderStatus.CANCELED, orderRepository.findByUuidIn(listOf(refused)).single().orderStatus)
        assertTrue(listOf(shop.team.master, shop.team.manager, shop.team.guest, buyer).all { notificationRepository.findAllByUserId(it.id!!).isEmpty() })

        // 오류 해제 후: 같은 빈·같은 executor 로 정상 저장. 앞선 주문들은 이미 승인·거절돼 늦게 도는 핸들러가 있어도 저장되지 않는다
        Thread.sleep(500)
        FAIL.set(false)
        val next = shop.order(newBuyer())
        val deadline = System.currentTimeMillis() + 10_000
        var saved: List<Notification> = emptyList()
        while (System.currentTimeMillis() < deadline) {
            saved = notificationRepository.findAllByUserId(shop.team.master.id!!).filter { it.type == NotificationType.ORDER_PENDING_APPROVE }
            if (saved.isNotEmpty()) break
            Thread.sleep(50)
        }
        assertEquals(listOf(next), saved.map { it.targetId })
    }

    /**
     * 실제 저장 로직을 상속하고, [FAIL] 이면 DB 오류를 던진다.
     * @Configuration / @TestConfiguration 을 붙이지 않는다: 통합 테스트 컴포넌트 스캔(band.gosrock)에 잡히면 다른 컨텍스트에도 @Primary 로 들어간다. @Import 로만 쓴다
     */
    class FailingBulkRepositoryConfig {
        @Bean
        @Primary
        fun failingBulkRepository(jdbcTemplate: JdbcTemplate): NotificationBulkRepository = object : NotificationBulkRepository(jdbcTemplate) {
            override fun insertSkippingDuplicates(notifications: List<Notification>, now: LocalDateTime): Int {
                ATTEMPTS.incrementAndGet()
                if (FAIL.get()) throw DataAccessResourceFailureException("알림 저장 DB 오류 (테스트)")
                return super.insertSkippingDuplicates(notifications, now)
            }
        }
    }

    companion object {
        private val FAIL = AtomicBoolean(false)
        private val ATTEMPTS = AtomicInteger(0)
    }
}
