package band.gosrock.api.v2.notification

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.operation.V2OperationTestSupport
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.notification.domain.Notification
import band.gosrock.domain.domains.notification.domain.NotificationTargetType
import band.gosrock.domain.domains.notification.domain.NotificationType
import band.gosrock.domain.domains.notification.repository.NotificationRepository
import band.gosrock.domain.domains.notification.service.v2.V2NotificationDomainService
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.user.domain.User
import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

/**
 * v2 알림센터 통합 테스트 (#714): 트리거별 저장(v1/v2 경로), 수신자, 중복 방지, N-1 ~ N-3, 권한.
 * 저장 핸들러는 커밋 후 비동기(@Async)라 [awaitNotifications] 로 저장될 때까지 기다린다 (최대 10초).
 * "저장 안 됨" 은 같은 요청이 만드는 다른 알림이 도착한 뒤(같은 스레드 풀) 또는 [NO_NOTIFICATION_WAIT_MS] 대기 후 확인하고, 서비스 직접 호출(0건)로도 확인한다.
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 알림센터")
class V2NotificationControllerTest : V2OperationTestSupport() {

    @Autowired private lateinit var notificationRepository: NotificationRepository

    @Autowired private lateinit var notificationDomainService: V2NotificationDomainService

    private fun notificationsOf(user: User, type: NotificationType? = null): List<Notification> =
        notificationRepository.findAllByUserId(user.id!!).filter { type == null || it.type == type }

    private fun awaitNotifications(user: User, type: NotificationType, count: Int = 1): List<Notification> {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            val found = notificationsOf(user, type)
            if (found.size >= count) return found
            Thread.sleep(50)
        }
        throw AssertionError("알림 $type ${count}건이 저장되지 않음 (userId=${user.id}, 현재 ${notificationsOf(user, type).size}건)")
    }

    private fun waitForNoNotification() = Thread.sleep(NO_NOTIFICATION_WAIT_MS)

    private fun v1Refuse(host: User, eventId: Long, orderUuid: String, reason: String?) =
        mockMvc.post("/api/v1/events/$eventId/orders/$orderUuid/refuse") {
            with(auth(host))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("reason" to reason))
        }

    private fun list(user: User?, page: Int = 0, size: Int = 20) =
        v2Get(user, "/me/notifications", mapOf("page" to page.toString(), "size" to size.toString()))

    private fun unreadCount(user: User): Long =
        v2Get(user, "/me/notifications/unread-count").andExpect { status { isOk() } }.data().at("/count").asLong()

    private fun read(user: User?, body: Map<String, Any?>) = v2Post(user, "/me/notifications/read", body)

    private fun saveNotification(user: User, key: String): Notification = notificationRepository.save(
        Notification(
            userId = user.id!!,
            type = NotificationType.ORDER_APPROVED,
            title = "제목-$key",
            body = "본문-$key",
            targetType = NotificationTargetType.ORDER,
            targetId = key,
            eventId = 1L,
            extra = """{"eventName":"공연"}""",
            dedupKey = key,
        ),
    )

    @Nested
    @DisplayName("저장 트리거")
    inner class Triggers {

        @Test
        fun `v2 멤버 추가 - 추가된 매니저·일반 멤버 각각 HOST_MEMBER_ADDED (딥링크 HOST), 추가한 마스터는 없음, 재처리 중복 없음`() {
            val team = Team()
            val manager = awaitNotifications(team.manager, NotificationType.HOST_MEMBER_ADDED).single()
            val guest = awaitNotifications(team.guest, NotificationType.HOST_MEMBER_ADDED).single()
            assertEquals(NotificationTargetType.HOST, manager.targetType)
            assertEquals(team.hostId.toString(), manager.targetId)
            assertNull(manager.eventId)
            assertTrue(manager.body.contains("'고스락'") && manager.body.contains("매니저"), manager.body)
            assertTrue(guest.body.contains("게스트"), guest.body)
            assertTrue(manager.extra!!.contains("\"hostName\":\"고스락\""))
            assertFalse(manager.isRead)
            assertTrue(notificationsOf(team.master).isEmpty())

            // 같은 이벤트 재처리 → 0건 (uk)
            assertEquals(0, notificationDomainService.notifyHostMembersAdded(team.hostId, listOf(team.manager.id!!, team.guest.id!!)))
            assertEquals(1, notificationsOf(team.manager, NotificationType.HOST_MEMBER_ADDED).size)
        }

        @Test
        fun `멤버 추가 실패(롤백) 시 알림 없음`() {
            val team = Team()
            awaitNotifications(team.manager, NotificationType.HOST_MEMBER_ADDED)
            val newbie = newUser("신입")
            // 이미 멤버인 이메일이 섞이면 전체 거절 → 롤백 → AFTER_COMMIT 핸들러 미실행
            v2Post(
                team.master,
                "/hosts/${team.hostId}/members",
                mapOf("members" to listOf(mapOf("email" to newbie.profile!!.email, "role" to "GUEST"), mapOf("email" to team.guest.profile!!.email, "role" to "GUEST"))),
            ).andExpect { status { isBadRequest() } }
            waitForNoNotification()
            assertTrue(notificationsOf(newbie).isEmpty())
        }

        @Test
        fun `v1 승인형 주문 생성 - 활성 마스터·매니저 각각 ORDER_PENDING_APPROVE, 일반 멤버·주문자는 없음, 재처리 중복 없음`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = shop.order(buyer)

            val master = awaitNotifications(shop.team.master, NotificationType.ORDER_PENDING_APPROVE).single()
            val manager = awaitNotifications(shop.team.manager, NotificationType.ORDER_PENDING_APPROVE).single()
            listOf(master, manager).forEach {
                assertEquals(NotificationTargetType.ORDER, it.targetType)
                assertEquals(orderUuid, it.targetId)
                assertEquals(shop.eventId, it.eventId)
            }
            assertTrue(master.body.contains("'정기공연'"), master.body)
            // 한 문장으로 같이 저장되므로 마스터 알림이 있으면 일반 멤버 몫도 이미 결정됨
            assertTrue(notificationsOf(shop.team.guest, NotificationType.ORDER_PENDING_APPROVE).isEmpty())
            assertTrue(notificationsOf(shop.team.outsider).isEmpty())
            assertTrue(notificationsOf(buyer).isEmpty())

            assertEquals(0, notificationDomainService.notifyOrderPendingApprove(orderUuid))
            assertEquals(1, notificationsOf(shop.team.master, NotificationType.ORDER_PENDING_APPROVE).size)
        }

        @Test
        fun `v1 승인 - 주문자 ORDER_APPROVED`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = shop.approved(buyer)
            val n = awaitNotifications(buyer, NotificationType.ORDER_APPROVED).single()
            assertEquals(orderUuid, n.targetId)
            assertEquals(shop.eventId, n.eventId)
            assertEquals("티켓 주문이 승인되었습니다!", n.title)
            assertEquals(0, notificationDomainService.notifyOrderApproved(orderUuid))
        }

        @Test
        fun `v2 승인 - 주문자 ORDER_APPROVED`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = shop.order(buyer)
            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$orderUuid/approve").andExpect { status { isOk() } }
            assertEquals(orderUuid, awaitNotifications(buyer, NotificationType.ORDER_APPROVED).single().targetId)
        }

        @Test
        fun `v2 거절 - 주문자 ORDER_REFUSED, 사유 종류·문구 포함`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = shop.order(buyer)
            refuse(shop.team.manager, shop.eventId, orderUuid, "ETC", "  좌석 배치 변경 ").andExpect { status { isOk() } }

            val n = awaitNotifications(buyer, NotificationType.ORDER_REFUSED).single()
            assertEquals(orderUuid, n.targetId)
            // 유료 계좌이체 거절 → 환불 계좌 입력 안내가 뒤에 붙는다 (#728)
            assertTrue(n.body.endsWith("사유: 좌석 배치 변경. 주문상세에서 환불 계좌를 입력해 주세요."), n.body)
            assertTrue(n.extra!!.contains("\"refuseReasonType\":\"ETC\""), n.extra)
            assertTrue(n.extra!!.contains("\"refuseReason\":\"좌석 배치 변경\""), n.extra)
            assertTrue(notificationsOf(buyer, NotificationType.ORDER_APPROVED).isEmpty())

            val res = list(buyer).andExpect { status { isOk() } }.data().at("/content/0")
            assertEquals("ETC", res.at("/extra/refuseReasonType").asText())
            assertEquals("좌석 배치 변경", res.at("/extra/refuseReason").asText())
        }

        @Test
        fun `v1 거절 - 주문자 ORDER_REFUSED (사유 종류 없음, v1 사유 문구), v1 응답 그대로`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = shop.order(buyer)
            v1Refuse(shop.team.master, shop.eventId, orderUuid, "입금 확인 불가").andExpect {
                status { isOk() }
                jsonPath("$.data.orderUuid") { value(orderUuid) }
            }

            val n = awaitNotifications(buyer, NotificationType.ORDER_REFUSED).single()
            // 유료 계좌이체 거절 → 환불 계좌 입력 안내가 뒤에 붙는다 (#728)
            assertTrue(n.body.endsWith("사유: 입금 확인 불가. 주문상세에서 환불 계좌를 입력해 주세요."), n.body)
            assertFalse(n.extra!!.contains("refuseReasonType"), n.extra)
        }

        @Test
        fun `v1 거절 사유 없음 - 사유 없이 저장`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = shop.order(buyer)
            v1Refuse(shop.team.master, shop.eventId, orderUuid, null).andExpect { status { isOk() } }
            val n = awaitNotifications(buyer, NotificationType.ORDER_REFUSED).single()
            assertFalse(n.body.contains("사유"), n.body)
        }

        @Test
        fun `승인 후 취소는 ORDER_REFUSED 아님 (승인 알림만)`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = shop.approved(buyer)
            awaitNotifications(buyer, NotificationType.ORDER_APPROVED)
            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$orderUuid/cancel").andExpect { status { isOk() } }
            waitForNoNotification()
            assertTrue(notificationsOf(buyer, NotificationType.ORDER_REFUSED).isEmpty())
            assertEquals(0, notificationDomainService.notifyOrderRefused(orderUuid))
        }

        @Test
        fun `결제형(무료 선착순) 주문 생성·확정은 알림 없음`() {
            val team = Team()
            val ticketId = createTicket(team.manager, team.eventId, freeBody(approvalRequired = false))
            setEventStatus(team.eventId, EventStatus.OPEN)
            awaitNotifications(team.manager, NotificationType.HOST_MEMBER_ADDED)
            val buyer = newBuyer()
            val orderUuid = v1Buy(buyer, team.master, team.eventId, ticketId, approval = false)
            assertEquals(OrderStatus.APPROVED, orderRepository.findByUuidIn(listOf(orderUuid)).single().orderStatus)

            waitForNoNotification()
            assertTrue(notificationsOf(buyer).isEmpty())
            assertTrue(notificationsOf(team.master).isEmpty())
            assertEquals(0, notificationDomainService.notifyOrderPendingApprove(orderUuid))
            assertEquals(0, notificationDomainService.notifyOrderApproved(orderUuid))
        }
    }

    @Nested
    @DisplayName("N-1 ~ N-3")
    inner class Api {

        @Test
        fun `N-1 최신 순 Slice, 필드, 페이징`() {
            val me = newUser("나")
            val keys = (1..3).map { "n1-${me.id}-$it" }
            val saved = keys.map { saveNotification(me, it) }

            val first = list(me, page = 0, size = 2).andExpect { status { isOk() } }.data()
            assertEquals(listOf(saved[2].id, saved[1].id), first.at("/content").map { it.at("/id").asLong() })
            assertTrue(first.at("/hasNext").asBoolean())
            assertTrue(first.at("/totalElements").isNull)
            val e: JsonNode = first.at("/content/0")
            assertEquals("ORDER_APPROVED", e.at("/type").asText())
            assertEquals("제목-${keys[2]}", e.at("/title").asText())
            assertEquals("본문-${keys[2]}", e.at("/body").asText())
            assertEquals("ORDER", e.at("/target/type").asText())
            assertEquals(keys[2], e.at("/target/id").asText())
            assertEquals(1L, e.at("/target/eventId").asLong())
            assertEquals("공연", e.at("/extra/eventName").asText())
            assertFalse(e.at("/isRead").asBoolean())
            assertTrue(e.at("/createdAt").asText().matches(Regex("""\d{4}\.\d{2}\.\d{2} \d{2}:\d{2}""")), e.at("/createdAt").asText())

            val second = list(me, page = 1, size = 2).andExpect { status { isOk() } }.data()
            assertEquals(listOf(saved[0].id), second.at("/content").map { it.at("/id").asLong() })
            assertFalse(second.at("/hasNext").asBoolean())

            // 남의 알림은 보이지 않음
            assertEquals(0, list(newUser()).andExpect { status { isOk() } }.data().at("/content").size())
            list(me, size = 101).andExpect { status { isBadRequest() } }
        }

        @Test
        fun `N-2 안읽음 수 · N-3 id 읽음 (남의 id 무시, 멱등) · 전체 읽음 (멱등)`() {
            val me = newUser("나")
            val other = newUser("남")
            val mine = (1..3).map { saveNotification(me, "n3-${me.id}-$it") }
            val others = saveNotification(other, "n3-${other.id}")
            assertEquals(3, unreadCount(me))

            // 내 것 1개 + 남의 것 1개 + 없는 id → 내 것만
            read(me, mapOf("notificationIds" to listOf(mine[0].id, others.id, 999_999_999L))).andExpect {
                status { isOk() }
                jsonPath("$.data.updatedCount") { value(1) }
                jsonPath("$.data.unreadCount") { value(2) }
            }
            assertFalse(notificationRepository.findById(others.id!!).get().isRead)
            assertEquals(1, unreadCount(other))
            val readOne = notificationRepository.findById(mine[0].id!!).get()
            assertTrue(readOne.isRead)
            assertTrue(readOne.readAt != null)

            // 같은 id 다시 → 0건 (멱등, 200)
            read(me, mapOf("notificationIds" to listOf(mine[0].id))).andExpect {
                status { isOk() }
                jsonPath("$.data.updatedCount") { value(0) }
            }
            assertTrue(list(me).data().at("/content").first { it.at("/id").asLong() == mine[0].id }.at("/isRead").asBoolean())

            // 남이 내 알림 읽음 처리 시도 → 무시
            read(other, mapOf("notificationIds" to listOf(mine[1].id))).andExpect { jsonPath("$.data.updatedCount") { value(0) } }
            assertEquals(2, unreadCount(me))

            // 빈 요청 → 0건
            read(me, emptyMap()).andExpect { jsonPath("$.data.updatedCount") { value(0) } }

            // 전체 읽음 → 나머지 2건, 다시 → 0건. 남의 것은 그대로
            read(me, mapOf("all" to true)).andExpect {
                jsonPath("$.data.updatedCount") { value(2) }
                jsonPath("$.data.unreadCount") { value(0) }
            }
            read(me, mapOf("all" to true)).andExpect {
                status { isOk() }
                jsonPath("$.data.updatedCount") { value(0) }
            }
            assertEquals(0, unreadCount(me))
            assertEquals(1, unreadCount(other))

            // 최대 100개
            read(me, mapOf("notificationIds" to (1L..101L).toList())).andExpect { status { isBadRequest() } }
        }

        @Test
        fun `비로그인 401`() {
            list(null).andExpect { status { isUnauthorized() } }
            v2Get(null, "/me/notifications/unread-count").andExpect { status { isUnauthorized() } }
            read(null, mapOf("all" to true)).andExpect { status { isUnauthorized() } }
        }

        @Test
        fun `트리거 → 목록·안읽음 수 연동 - 호스트 매니저, 멤버 추가 + 승인 대기 주문`() {
            val shop = Shop()
            awaitNotifications(shop.team.manager, NotificationType.HOST_MEMBER_ADDED)
            val orderUuid = shop.order(newBuyer())
            awaitNotifications(shop.team.manager, NotificationType.ORDER_PENDING_APPROVE)

            assertEquals(2, unreadCount(shop.team.manager))
            val content = list(shop.team.manager).andExpect { status { isOk() } }.data().at("/content")
            assertEquals(listOf("ORDER_PENDING_APPROVE", "HOST_MEMBER_ADDED"), content.map { it.at("/type").asText() })
            assertEquals(orderUuid, content[0].at("/target/id").asText())
            assertEquals(shop.eventId, content[0].at("/target/eventId").asLong())
            assertEquals(shop.team.hostId.toString(), content[1].at("/target/id").asText())
            assertTrue(content[1].at("/target/eventId").isNull)
        }
    }

    companion object {
        private const val NO_NOTIFICATION_WAIT_MS = 1_500L
    }
}
