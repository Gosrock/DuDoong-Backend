package band.gosrock.api.v2.ticket

import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import band.gosrock.domain.domains.ticket_item.repository.TicketItemRepository
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.OauthProvider
import band.gosrock.domain.domains.user.domain.Profile
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.domain.domains.user.repository.UserRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

/**
 * v2 티켓/옵션 통합 테스트 공통 픽스처 (#707).
 * 컨텍스트(H2)가 테스트 간에 공유되므로 테스트마다 유저/호스트/공연을 새로 만든다.
 * v1 구매 플로우(카트 → 주문 → 무료 확정 / 호스트 승인)는 Redis(Redisson 락)가 필요하다.
 */
abstract class V2TicketApiTestSupport {

    @Autowired protected lateinit var mockMvc: MockMvc

    @Autowired protected lateinit var objectMapper: ObjectMapper

    @Autowired protected lateinit var userRepository: UserRepository

    @Autowired protected lateinit var eventRepository: EventRepository

    @Autowired protected lateinit var ticketItemRepository: TicketItemRepository

    private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

    protected fun LocalDateTime.f(): String = format(fmt)

    /** 공연 시작: 30일 뒤 18:00 */
    protected val eventStart: LocalDateTime = LocalDate.now().plusDays(30).atTime(18, 0)

    protected fun newUser(name: String = "유저"): User =
        userRepository.save(
            User(
                profile = Profile(name = name, email = "v2ticket-${UUID.randomUUID()}@test.com", phoneNumber = null, profileImage = null),
                oauthInfo = OauthInfo(OauthProvider.KAKAO, "v2ticket-${UUID.randomUUID()}"),
            ),
        )

    protected fun superAdmin(): User = newUser("관리자").also { it.changeRole(AccountRole.SUPER_ADMIN) }.let { userRepository.save(it) }

    protected fun auth(u: User) = user(u.id.toString()).roles("USER")

    protected fun json(body: Any?): String = objectMapper.writeValueAsString(body)

    protected fun ResultActionsDsl.body(): JsonNode =
        objectMapper.readTree(andReturn().response.getContentAsString(Charsets.UTF_8))

    protected fun ResultActionsDsl.data(): JsonNode = body().at("/data")

    protected fun createHost(master: User, name: String = "고스락"): Long =
        mockMvc.post("/api/v2/hosts") {
            with(auth(master))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("name" to name, "contacts" to listOf(mapOf("type" to "EMAIL", "value" to "h@gosrock.band"))))
        }.andExpect { status { isOk() } }.body().at("/data/hostId").asLong()

    /** 마스터 + 매니저 + 일반 + 외부인 + 공연 1개 */
    protected inner class Team(hostName: String = "고스락", hasTicket: Boolean = true) {
        val master = newUser("마스터")
        val manager = newUser("매니저")
        val guest = newUser("일반")
        val outsider = newUser("외부인")
        val hostId = createHost(master, hostName)
        val eventId: Long

        init {
            mockMvc.post("/api/v2/hosts/$hostId/members") {
                with(auth(master))
                contentType = MediaType.APPLICATION_JSON
                content = json(
                    mapOf(
                        "members" to listOf(
                            mapOf("email" to manager.profile!!.email, "role" to "MANAGER"),
                            mapOf("email" to guest.profile!!.email, "role" to "GUEST"),
                        ),
                    ),
                )
            }.andExpect { status { isOk() } }
            eventId = createEvent(hasTicket)
        }

        fun createEvent(hasTicket: Boolean = true): Long =
            mockMvc.post("/api/v2/events") {
                with(auth(master))
                contentType = MediaType.APPLICATION_JSON
                content = json(
                    mapOf("hostId" to hostId, "name" to "정기공연", "startAt" to eventStart.f(), "endAt" to eventStart.plusHours(2).f(), "hasTicket" to hasTicket),
                )
            }.andExpect { status { isOk() } }.body().at("/data/eventId").asLong()
    }

    protected fun setEventStatus(eventId: Long, status: EventStatus) {
        val event = eventRepository.findById(eventId).get()
        ReflectionTestUtils.setField(event, "status", status)
        eventRepository.save(event)
    }

    // ===== 티켓 =====

    protected val account = mapOf("bank" to "신한은행", "holder" to "고스락", "number" to "110-123-456789")

    protected fun dudoongBody(
        name: String = "일반",
        price: Long = 6000,
        supplyCount: Long? = 100,
        overrides: Map<String, Any?> = emptyMap(),
    ): Map<String, Any?> = mapOf(
        "payType" to "DUDOONG",
        "name" to name,
        "description" to "일반 입장",
        "price" to price,
        "supplyCount" to supplyCount,
        "account" to account,
        "approvalRequired" to true,
        "isQuantityPublic" to true,
        "purchaseLimit" to 4,
        "saleStartAt" to null,
        "saleEndAt" to null,
    ) + overrides

    protected fun freeBody(name: String = "무료", supplyCount: Long? = 10, approvalRequired: Boolean = false, overrides: Map<String, Any?> = emptyMap()) =
        dudoongBody(name = name, price = 0, supplyCount = supplyCount, overrides = mapOf("payType" to "FREE", "account" to null, "approvalRequired" to approvalRequired) + overrides)

    protected fun postTicket(requester: User, eventId: Long, body: Map<String, Any?>): ResultActionsDsl =
        mockMvc.post("/api/v2/events/$eventId/ticket-items") {
            with(auth(requester))
            contentType = MediaType.APPLICATION_JSON
            content = json(body)
        }

    protected fun createTicket(requester: User, eventId: Long, body: Map<String, Any?> = dudoongBody()): Long =
        postTicket(requester, eventId, body).andExpect { status { isOk() } }.data().at("/ticketItemId").asLong()

    protected fun patchTicket(requester: User, eventId: Long, ticketItemId: Long, body: Map<String, Any?>): ResultActionsDsl =
        mockMvc.patch("/api/v2/events/$eventId/ticket-items/$ticketItemId") {
            with(auth(requester))
            contentType = MediaType.APPLICATION_JSON
            content = json(body)
        }

    protected fun deleteTicket(requester: User, eventId: Long, ticketItemId: Long): ResultActionsDsl =
        mockMvc.delete("/api/v2/events/$eventId/ticket-items/$ticketItemId") { with(auth(requester)) }

    protected fun suspend(requester: User, eventId: Long, ticketItemId: Long): ResultActionsDsl =
        mockMvc.post("/api/v2/events/$eventId/ticket-items/$ticketItemId/suspend") { with(auth(requester)) }

    protected fun resume(requester: User, eventId: Long, ticketItemId: Long): ResultActionsDsl =
        mockMvc.post("/api/v2/events/$eventId/ticket-items/$ticketItemId/resume") { with(auth(requester)) }

    protected fun manage(requester: User, eventId: Long): ResultActionsDsl =
        mockMvc.get("/api/v2/events/$eventId/ticket-items/manage") { with(auth(requester)) }

    protected fun manageItem(requester: User, eventId: Long, ticketItemId: Long): JsonNode =
        manage(requester, eventId).andExpect { status { isOk() } }.data().first { it.at("/ticketItemId").asLong() == ticketItemId }

    protected fun putOptions(requester: User, eventId: Long, ticketItemId: Long, optionIds: List<Long>): ResultActionsDsl =
        mockMvc.put("/api/v2/events/$eventId/ticket-items/$ticketItemId/options") {
            with(auth(requester))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("optionIds" to optionIds))
        }

    // ===== 옵션 =====

    protected fun postOption(requester: User, eventId: Long, body: Map<String, Any?>): ResultActionsDsl =
        mockMvc.post("/api/v2/events/$eventId/options") {
            with(auth(requester))
            contentType = MediaType.APPLICATION_JSON
            content = json(body)
        }

    protected fun createOption(requester: User, eventId: Long, type: String = "YES_NO", yesAdditionalPrice: Long? = 0, name: String = "뒷풀이"): Long =
        postOption(requester, eventId, mapOf("name" to name, "description" to "참석하나요?", "type" to type, "yesAdditionalPrice" to yesAdditionalPrice))
            .andExpect { status { isOk() } }.data().at("/optionId").asLong()

    protected fun patchOption(requester: User, eventId: Long, optionId: Long, body: Map<String, Any?>): ResultActionsDsl =
        mockMvc.patch("/api/v2/events/$eventId/options/$optionId") {
            with(auth(requester))
            contentType = MediaType.APPLICATION_JSON
            content = json(body)
        }

    protected fun deleteOption(requester: User, eventId: Long, optionId: Long): ResultActionsDsl =
        mockMvc.delete("/api/v2/events/$eventId/options/$optionId") { with(auth(requester)) }

    protected fun options(requester: User, eventId: Long): ResultActionsDsl =
        mockMvc.get("/api/v2/events/$eventId/options") { with(auth(requester)) }

    // ===== v1 구매 플로우 =====

    /** v1 GET .../ticketItems/{id}/options 로 옵션 행 id 를 얻어 답변을 만든다 (네/아니오는 '예', 주관식은 '홍길동') */
    protected fun v1Answers(buyer: User, eventId: Long, ticketItemId: Long): List<Map<String, Any>> =
        mockMvc.get("/api/v1/events/$eventId/ticketItems/$ticketItemId/options") { with(auth(buyer)) }
            .andExpect { status { isOk() } }.data().at("/optionGroups").map { group ->
                val rows = group.at("/options")
                if (rows.size() == 1) {
                    mapOf("optionId" to rows[0].at("/optionId").asLong(), "answer" to "홍길동")
                } else {
                    mapOf("optionId" to rows.first { it.at("/answer").asText() == "예" }.at("/optionId").asLong(), "answer" to "예")
                }
            }

    protected fun v1Cart(buyer: User, ticketItemId: Long, quantity: Long = 1, answers: List<Map<String, Any>> = emptyList()): ResultActionsDsl =
        mockMvc.post("/api/v1/carts") {
            with(auth(buyer))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("items" to listOf(mapOf("itemId" to ticketItemId, "quantity" to quantity, "options" to answers))))
        }

    /**
     * v1 사용자 구매: 카트 → 주문 → (무료 선착순) 무료 확정 / (승인형) 호스트 승인. 재고가 감소한다.
     * 공연은 OPEN 이어야 한다.
     */
    /** v1 카트 → 주문 생성 (확정·승인 전). 승인형이면 PENDING_APPROVE, 무료 선착순이면 PENDING_PAYMENT */
    protected fun v1Order(buyer: User, eventId: Long, ticketItemId: Long, quantity: Long = 1): String {
        val answers = v1Answers(buyer, eventId, ticketItemId)
        val cartId = v1Cart(buyer, ticketItemId, quantity, answers).andExpect { status { isOk() } }.data().at("/cartId").asLong()
        return v1CreateOrder(buyer, cartId).andExpect { status { isOk() } }.data().at("/orderId").asText()
    }

    protected fun v1CreateOrder(buyer: User, cartId: Long): ResultActionsDsl =
        mockMvc.post("/api/v1/orders/") {
            with(auth(buyer))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("cartId" to cartId, "couponId" to null))
        }

    protected fun v1Approve(host: User, eventId: Long, orderUuid: String): ResultActionsDsl =
        mockMvc.post("/api/v1/events/$eventId/orders/$orderUuid/approve") { with(auth(host)) }

    protected fun v1FreeConfirm(buyer: User, orderUuid: String): ResultActionsDsl =
        mockMvc.post("/api/v1/orders/$orderUuid/free") { with(auth(buyer)) }

    /**
     * v1 사용자 구매: 카트 → 주문 → (무료 선착순) 무료 확정 / (승인형) 호스트 승인. 재고가 감소한다.
     * 공연은 OPEN 이어야 한다.
     */
    protected fun v1Buy(buyer: User, host: User, eventId: Long, ticketItemId: Long, quantity: Long = 1, approval: Boolean): String {
        val orderUuid = v1Order(buyer, eventId, ticketItemId, quantity)
        if (approval) {
            v1Approve(host, eventId, orderUuid).andExpect { status { isOk() } }
        } else {
            v1FreeConfirm(buyer, orderUuid).andExpect { status { isOk() } }
        }
        return orderUuid
    }

    /** v1 API 로 만든 것과 같은 티켓(v1 매퍼와 같은 값)을 직접 저장 */
    protected fun saveV1FreeTicket(eventId: Long, supplyCount: Long = 10, name: String = "v1무료", isSellable: Boolean? = true): TicketItem =
        ticketItemRepository.save(
            TicketItem(
                payType = TicketPayType.FREE_TICKET,
                name = name,
                description = "v1",
                price = Money.ZERO,
                quantity = supplyCount,
                supplyCount = supplyCount,
                purchaseLimit = 2L,
                type = TicketType.FIRST_COME_FIRST_SERVED,
                isQuantityPublic = true,
                isSellable = isSellable,
                eventId = eventId,
            ),
        )
}
