package band.gosrock.api.v2.gift

import band.gosrock.api.v2.order.V2UserOrderTestSupport
import band.gosrock.domain.domains.gift.domain.TicketGift
import band.gosrock.domain.domains.gift.repository.TicketGiftRepository
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.notification.domain.NotificationType
import band.gosrock.domain.domains.notification.repository.NotificationRepository
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.User
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

/**
 * v2 티켓탭·선물 통합 테스트 공통 (#719). [Shop] 의 두둥티켓(승인형, 1인 4장)을 v2 로 주문하고 v1 호스트 승인으로 발급한다.
 * H2 주의([band.gosrock.api.v2.operation.V2OperationTestSupport]): 승인 예정 주문은 2장 이하
 */
abstract class V2GiftTestSupport : V2UserOrderTestSupport() {

    @Autowired protected lateinit var ticketGiftRepository: TicketGiftRepository

    @Autowired protected lateinit var notificationRepository: NotificationRepository

    /** 승인(발급)된 v2 주문 → (orderUuid, 티켓 uuid 발급 순) */
    protected fun approvedOrder(shop: Shop, buyer: User, quantity: Long = 1): Pair<String, List<String>> {
        val orderUuid = v2OrderOk(buyer, shopBody(shop, quantity = quantity)).at("/orderUuid").asText()
        v1Approve(shop.team.master, shop.eventId, orderUuid).andExpect { status { isOk() } }
        return orderUuid to shop.ticketUuids(orderUuid)
    }

    protected fun ticketOf(id: Long): IssuedTicket = issuedTicketRepository.findById(id).get()

    protected fun ticketByUuid(uuid: String): IssuedTicket = issuedTicketRepository.findByUuid(uuid).get()

    protected fun giftOf(id: Long): TicketGift = ticketGiftRepository.findById(id).get()

    // ===== G-1 ~ G-8 =====

    protected fun gift(sender: User?, ticketUuid: String, memo: String? = null): ResultActionsDsl =
        v2Post(sender, "/me/tickets/$ticketUuid/gift", mapOf("memo" to memo))

    /** 성공해야 하는 G-1 → {giftId, giftToken, linkPath, memo} */
    protected fun giftOk(sender: User, ticketUuid: String, memo: String? = null): JsonNode =
        gift(sender, ticketUuid, memo).andExpect { status { isOk() } }.data()

    protected fun accept(receiver: User?, token: String): ResultActionsDsl = v2Post(receiver, "/gifts/$token/accept")

    protected fun reject(receiver: User?, token: String): ResultActionsDsl = v2Post(receiver, "/gifts/$token/reject")

    protected fun landing(viewer: User?, token: String): JsonNode =
        v2Get(viewer, "/gifts/$token").andExpect { status { isOk() } }.data()

    protected fun cancelGift(sender: User?, giftId: Long): ResultActionsDsl =
        mockMvc.delete("/api/v2/me/gifts/$giftId") { sender?.let { with(auth(it)) } }

    protected fun changeMemo(sender: User?, giftId: Long, memo: String?): ResultActionsDsl =
        mockMvc.patch("/api/v2/me/gifts/$giftId") {
            sender?.let { with(auth(it)) }
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("memo" to memo))
        }

    protected fun returnTicket(receiver: User?, ticketUuid: String): ResultActionsDsl = v2Post(receiver, "/me/tickets/$ticketUuid/return")

    protected fun gifts(user: User, direction: String): JsonNode =
        v2Get(user, "/me/gifts", mapOf("direction" to direction)).andExpect { status { isOk() } }.data()

    protected fun myTickets(user: User): JsonNode = v2Get(user, "/me/tickets").andExpect { status { isOk() } }.data().at("/groups")

    protected fun myTicket(user: User, ticketUuid: String): ResultActionsDsl = v2Get(user, "/me/tickets/$ticketUuid")

    /** 선물 생성 → 수락까지. @return (giftId, 받은 사람의 새 uuid) */
    protected fun giveAndAccept(sender: User, receiver: User, ticketUuid: String): Pair<Long, String> {
        val created = giftOk(sender, ticketUuid)
        val accepted = accept(receiver, created.at("/giftToken").asText()).andExpect { status { isOk() } }.data()
        return created.at("/giftId").asLong() to accepted.at("/ticketUuid").asText()
    }

    // ===== v1 경로 =====

    protected fun v1Entrance(host: User, eventId: Long, ticketUuid: String): ResultActionsDsl =
        mockMvc.patch("/api/v1/events/$eventId/issuedTickets/$ticketUuid") { with(auth(host)) }

    protected fun v1TicketDetail(user: User, ticketUuid: String): ResultActionsDsl = mockMvc.get("/api/v1/issuedTickets/$ticketUuid") { with(auth(user)) }

    protected fun v1OrderTickets(user: User, orderUuid: String): JsonNode =
        mockMvc.get("/api/v1/orders/$orderUuid/tickets") { with(auth(user)) }.andExpect { status { isOk() } }.data().at("/tickets")

    protected fun v1Refund(user: User, orderUuid: String): ResultActionsDsl = mockMvc.post("/api/v1/orders/$orderUuid/refund") { with(auth(user)) }

    protected fun v1HostCancel(host: User, eventId: Long, orderUuid: String): ResultActionsDsl =
        mockMvc.post("/api/v1/events/$eventId/orders/$orderUuid/cancel") { with(auth(host)) }

    protected fun v2HostCancel(host: User, eventId: Long, orderUuid: String): ResultActionsDsl =
        v2Post(host, "/events/$eventId/orders/$orderUuid/cancel", mapOf("reason" to "공연 취소"))

    // ===== 운영 어드민 =====

    protected fun newAdmin(): User = newUser("운영자").also { it.changeRole(AccountRole.ADMIN) }.let { userRepository.save(it) }

    private fun adminAuth(admin: User) = user(admin.id.toString()).roles("ADMIN")

    protected fun adminCancel(admin: User, orderUuid: String): ResultActionsDsl =
        mockMvc.post("/internal-api/v1/orders/$orderUuid/cancel") {
            with(adminAuth(admin))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("reason" to "운영 취소"))
        }

    protected fun adminUserStatus(admin: User, userId: Long, status: String): ResultActionsDsl =
        mockMvc.patch("/internal-api/v1/users/$userId/status") {
            with(adminAuth(admin))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("status" to status))
        }

    protected fun adminDeleteEvent(admin: User, eventId: Long): ResultActionsDsl =
        mockMvc.delete("/internal-api/v1/events/$eventId") { with(adminAuth(admin)) }

    protected fun adminEventStatus(admin: User, eventId: Long, status: String): ResultActionsDsl =
        mockMvc.patch("/internal-api/v1/events/$eventId/status") {
            with(adminAuth(admin))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("status" to status))
        }

    // ===== 시간·알림·동시성 =====

    /** 공연 시작 30분 전 → 진행 중(종료 90분 전, runTime 120) */
    protected fun startEvent(eventId: Long) = setEventStart(eventId, LocalDateTime.now().minusMinutes(30))

    /** 공연 종료 (시작 3시간 전, runTime 120) — OPEN 그대로라도 '선물 만료' */
    protected fun endEvent(eventId: Long) = setEventStart(eventId, LocalDateTime.now().minusHours(3))

    protected fun notificationCount(user: User, type: NotificationType) = notificationRepository.findAllByUserId(user.id!!).count { it.type == type }

    /** 처음 보일 때까지 기다린 뒤 0.5초 더 기다려 최종 개수를 다시 센다 */
    protected fun awaitNotification(user: User, type: NotificationType): Int {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline && notificationCount(user, type) == 0) Thread.sleep(50)
        Thread.sleep(500)
        return notificationCount(user, type)
    }

    /** n 개 작업을 동시에 시작해 결과 문자열(예외는 "ERR:...")을 모은다 */
    protected fun concurrently(vararg blocks: () -> String): List<String> {
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(blocks.size)
        val results = Collections.synchronizedList(mutableListOf<String>())
        blocks.forEach { block ->
            pool.submit {
                start.await()
                results += runCatching { block() }.getOrElse { "ERR:$it" }
            }
        }
        start.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS))
        return results
    }

    /** 성공이면 "OK", 실패면 에러 코드 */
    protected fun ResultActionsDsl.outcome(): String {
        val response = andReturn().response
        return if (response.status == 200) "OK" else body().at("/code").asText()
    }
}
