package band.gosrock.domain.domains.notification.service.v2

import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.notification.domain.Notification
import band.gosrock.domain.domains.notification.domain.NotificationTargetType
import band.gosrock.domain.domains.notification.domain.NotificationType
import band.gosrock.domain.domains.notification.repository.NotificationBulkRepository
import band.gosrock.domain.domains.notification.repository.NotificationRepository
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.domain.OrderRefuseReasonType
import band.gosrock.domain.domains.order.domain.OrderStatus
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils

/** v2 알림 저장 규칙 (#714): 수신자, 종류별 조건, 문구·딥링크·중복 키, 읽음 처리 */
class V2NotificationDomainServiceTest {

    private val notificationRepository = mock(NotificationRepository::class.java)
    private val saved = mutableListOf<Notification>()
    private val bulkRepository = mock(NotificationBulkRepository::class.java) { inv ->
        @Suppress("UNCHECKED_CAST")
        val list = inv.arguments.firstOrNull() as? List<Notification>
        list?.let { saved.addAll(it); it.size } ?: 0
    }
    private val hostAdaptor = mock(HostAdaptor::class.java)
    private val eventAdaptor = mock(EventAdaptor::class.java)
    private val orderAdaptor = mock(OrderAdaptor::class.java)
    private val service = V2NotificationDomainService(notificationRepository, bulkRepository, hostAdaptor, eventAdaptor, orderAdaptor)

    private val masterId = 1L
    private val managerId = 2L
    private val guestId = 3L
    private val pendingManagerId = 4L
    private val buyerId = 9L
    private lateinit var host: Host

    private fun hostUser(id: Long, userId: Long, role: HostRole, active: Boolean = true) =
        HostUser(host = host, userId = userId, role = role).also {
            ReflectionTestUtils.setField(it, "id", id)
            ReflectionTestUtils.setField(it, "active", active)
        }

    private fun order(
        status: OrderStatus,
        method: OrderMethod = OrderMethod.APPROVAL,
        approvedAt: LocalDateTime? = null,
        cancelReason: String? = null,
        reasonType: OrderRefuseReasonType? = null,
    ): Order = Order.forTest(userId = buyerId, orderName = "일반 1매", orderStatus = status, orderMethod = method, eventId = 50L, cancelReason = cancelReason).also {
        ReflectionTestUtils.setField(it, "uuid", "order-uuid")
        ReflectionTestUtils.setField(it, "orderNo", "R1000001")
        ReflectionTestUtils.setField(it, "approvedAt", approvedAt)
        ReflectionTestUtils.setField(it, "refuseReasonType", reasonType)
        `when`(orderAdaptor.findByOrderUuid("order-uuid")).thenReturn(it)
    }

    @BeforeEach
    fun setUp() {
        host = Host(masterUserId = masterId, name = "고스락")
        ReflectionTestUtils.setField(host, "id", 100L)
        host.hostUsers.addAll(
            setOf(
                hostUser(11L, masterId, HostRole.MASTER),
                hostUser(12L, managerId, HostRole.MANAGER),
                hostUser(13L, guestId, HostRole.GUEST),
                hostUser(14L, pendingManagerId, HostRole.MANAGER, active = false),
            ),
        )
        `when`(hostAdaptor.findById(100L)).thenReturn(host)
        val event = Event(hostId = 100L, name = "정기공연", startAt = LocalDateTime.now().plusDays(10), runTime = 60L)
        `when`(eventAdaptor.findById(50L)).thenReturn(event)
    }

    @Nested
    inner class HostMemberAdded {
        @Test
        fun `추가된 활성 멤버마다 HOST 딥링크, 중복 키는 host_user id, 문구에 호스트명·역할`() {
            assertEquals(2, service.notifyHostMembersAdded(100L, listOf(managerId, guestId, guestId)))
            assertEquals(listOf(managerId, guestId), saved.map { it.userId })
            val manager = saved[0]
            assertEquals(NotificationType.HOST_MEMBER_ADDED, manager.type)
            assertEquals(NotificationTargetType.HOST, manager.targetType)
            assertEquals("100", manager.targetId)
            assertNull(manager.eventId)
            assertEquals("host_user:12", manager.dedupKey)
            assertEquals("'고스락' 호스트에 매니저(으)로 추가되었습니다.", manager.body)
            assertEquals("""{"hostName":"고스락","role":"MANAGER"}""", manager.extra)
        }

        @Test
        fun `핸들러 실행 전에 삭제됐거나 비활성인 멤버는 건너뛴다`() {
            assertEquals(0, service.notifyHostMembersAdded(100L, listOf(pendingManagerId, 77L)))
            assertTrue(saved.isEmpty())
        }
    }

    @Nested
    inner class OrderNotifications {
        @Test
        fun `승인 대기 - 활성 마스터·매니저만 (일반 멤버·초대 대기 매니저 제외), ORDER 딥링크 + eventId, 중복 키 orderUuid`() {
            order(OrderStatus.PENDING_APPROVE)
            assertEquals(2, service.notifyOrderPendingApprove("order-uuid"))
            assertEquals(setOf(masterId, managerId), saved.map { it.userId }.toSet())
            saved.forEach {
                assertEquals(NotificationType.ORDER_PENDING_APPROVE, it.type)
                assertEquals(NotificationTargetType.ORDER, it.targetType)
                assertEquals("order-uuid", it.targetId)
                assertEquals(50L, it.eventId)
                assertEquals("order-uuid", it.dedupKey)
                assertEquals("'정기공연' 일반 1매 주문(R1000001)이 접수되었습니다.", it.body)
            }
        }

        @Test
        fun `결제형 주문, 이미 처리된 승인형 주문은 승인 대기 알림 없음`() {
            order(OrderStatus.PENDING_PAYMENT, method = OrderMethod.PAYMENT)
            assertEquals(0, service.notifyOrderPendingApprove("order-uuid"))
            order(OrderStatus.APPROVED, approvedAt = LocalDateTime.now())
            assertEquals(0, service.notifyOrderPendingApprove("order-uuid"))
            assertTrue(saved.isEmpty())
        }

        @Test
        fun `승인 - 승인형 승인 완료만 주문자에게, 결제형 확정·승인 후 취소는 없음`() {
            order(OrderStatus.APPROVED, approvedAt = LocalDateTime.now())
            assertEquals(1, service.notifyOrderApproved("order-uuid"))
            assertEquals(buyerId, saved.single().userId)
            assertEquals(NotificationType.ORDER_APPROVED, saved.single().type)

            order(OrderStatus.APPROVED, method = OrderMethod.PAYMENT, approvedAt = LocalDateTime.now())
            assertEquals(0, service.notifyOrderApproved("order-uuid"))
            order(OrderStatus.CANCELED, approvedAt = LocalDateTime.now())
            assertEquals(0, service.notifyOrderApproved("order-uuid"))
        }

        @Test
        fun `거절 - v2 거절은 사유 종류·문구, v1 거절은 문구만`() {
            order(OrderStatus.CANCELED, cancelReason = "티켓 매진", reasonType = OrderRefuseReasonType.SOLD_OUT)
            assertEquals(1, service.notifyOrderRefused("order-uuid"))
            val v2 = saved.removeAt(0)
            assertEquals(NotificationType.ORDER_REFUSED, v2.type)
            assertEquals(buyerId, v2.userId)
            assertEquals("'정기공연' 일반 1매 주문이 거절되었어요. 사유: 티켓 매진", v2.body)
            assertEquals("""{"eventName":"정기공연","orderNo":"R1000001","refuseReasonType":"SOLD_OUT","refuseReason":"티켓 매진"}""", v2.extra)

            order(OrderStatus.CANCELED, cancelReason = " ")
            assertEquals(1, service.notifyOrderRefused("order-uuid"))
            val v1 = saved.single()
            assertEquals("'정기공연' 일반 1매 주문이 거절되었어요.", v1.body)
            assertFalse(v1.extra!!.contains("refuse"))
        }

        @Test
        fun `승인 후 취소·환불은 거절 알림 없음`() {
            order(OrderStatus.CANCELED, approvedAt = LocalDateTime.now(), cancelReason = "일정 변경")
            assertEquals(0, service.notifyOrderRefused("order-uuid"))
            order(OrderStatus.REFUND, approvedAt = LocalDateTime.now())
            assertEquals(0, service.notifyOrderRefused("order-uuid"))
            assertTrue(saved.isEmpty())
        }

        @Test
        fun `긴 문구는 컬럼 길이로 자른다`() {
            order(OrderStatus.CANCELED, cancelReason = "가".repeat(500))
            service.notifyOrderRefused("order-uuid")
            assertEquals(Notification.BODY_MAX_LENGTH, saved.single().body.length)
            assertTrue(saved.single().extra!!.length <= Notification.EXTRA_MAX_LENGTH)
        }
    }

    @Nested
    inner class MarkRead {
        /** 호출된 쿼리 메서드 이름과 인자를 기록하고, 바뀐 건수로 all=3, id 목록=중복 제거한 개수를 돌려준다 */
        private val calls = mutableListOf<Pair<String, List<Any?>>>()
        private val repository = mock(NotificationRepository::class.java) { inv ->
            calls.add(inv.method.name to inv.arguments.toList())
            when (inv.method.name) {
                "markAllRead" -> 3
                "markReadByIds" -> (inv.arguments[1] as Collection<*>).size
                else -> null
            }
        }
        private val s = V2NotificationDomainService(repository, bulkRepository, hostAdaptor, eventAdaptor, orderAdaptor)

        @Test
        fun `all 이면 전체 (id 목록 무시)`() {
            assertEquals(3, s.markRead(1L, listOf(5L), all = true))
            assertEquals(listOf("markAllRead"), calls.map { it.first })
            assertEquals(1L, calls.single().second[0])
        }

        @Test
        fun `id 목록은 본인 조건으로, 중복 제거`() {
            assertEquals(2, s.markRead(1L, listOf(5L, 6L, 5L), all = false))
            val (name, args) = calls.single()
            assertEquals("markReadByIds", name)
            assertEquals(1L, args[0])
            assertEquals(setOf(5L, 6L), args[1])
        }

        @Test
        fun `둘 다 없으면 아무것도 안 함`() {
            assertEquals(0, s.markRead(1L, null, all = false))
            assertEquals(0, s.markRead(1L, emptyList(), all = false))
            assertTrue(calls.isEmpty())
        }
    }
}
