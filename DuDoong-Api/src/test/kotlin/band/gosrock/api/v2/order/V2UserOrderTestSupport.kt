package band.gosrock.api.v2.order

import band.gosrock.api.v2.operation.V2OperationTestSupport
import band.gosrock.domain.domains.cart.repository.CartRepository
import band.gosrock.domain.domains.event.domain.EventBasic
import band.gosrock.domain.domains.order.repository.OrderRefundAccountRepository
import band.gosrock.domain.domains.user.domain.User
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.ResultActionsDsl

/**
 * v2 사용자 주문 통합 테스트 공통 (#718). [V2OperationTestSupport.Shop] = 두둥티켓 6000원(재고 20, 1인 4장, 승인) + 옵션(네/아니오 +1000 '뒷풀이', 주관식 '입금자명').
 * H2 주의([V2OperationTestSupport] 참고): 승인 예정 주문은 2장 이하로 만든다
 */
abstract class V2UserOrderTestSupport : V2OperationTestSupport() {

    @Autowired protected lateinit var refundAccountRepository: OrderRefundAccountRepository

    @Autowired protected lateinit var cartRepository: CartRepository

    protected val refundAccount = mapOf("bankName" to "국민은행", "accountHolder" to "홍길동", "accountNumber" to "123-45-678901")

    protected fun answers(shop: Shop, yes: Boolean = true, text: String = "홍길동") = listOf(
        mapOf("optionId" to shop.yesNoOptionId, "answer" to if (yes) "YES" else "NO"),
        mapOf("optionId" to shop.subjectiveOptionId, "answer" to text),
    )

    protected fun orderBody(
        eventId: Long,
        ticketItemId: Long,
        quantity: Long = 1,
        answers: List<Map<String, Any?>> = emptyList(),
        perTicket: List<List<Map<String, Any?>>>? = null,
        method: String = "BANK_TRANSFER",
        depositorName: String? = "입금자",
        agree: Boolean? = true,
    ): Map<String, Any?> = mapOf(
        "eventId" to eventId,
        "ticketItemId" to ticketItemId,
        "quantity" to quantity,
        "options" to mapOf("applyToAll" to (perTicket == null), "answers" to answers),
        "perTicketOptions" to perTicket,
        "paymentMethod" to method,
        "depositorName" to depositorName,
        "agreeRefundPolicy" to agree,
    )

    protected fun shopBody(shop: Shop, quantity: Long = 1, yes: Boolean = true, method: String = "BANK_TRANSFER", depositorName: String? = "입금자") =
        orderBody(shop.eventId, shop.ticketId, quantity, answers(shop, yes), method = method, depositorName = depositorName)

    protected fun v2CreateOrder(buyer: User?, body: Map<String, Any?>): ResultActionsDsl = v2Post(buyer, "/orders", body)

    /** 성공해야 하는 주문 → 주문 상세(data) */
    protected fun v2OrderOk(buyer: User, body: Map<String, Any?>): JsonNode =
        v2CreateOrder(buyer, body).andExpect { status { isOk() } }.data()

    protected fun myOrder(buyer: User, orderUuid: String): ResultActionsDsl = v2Get(buyer, "/me/orders/$orderUuid")

    protected fun myOrders(buyer: User, params: Map<String, String> = emptyMap()): JsonNode =
        v2Get(buyer, "/me/orders", params).andExpect { status { isOk() } }.data()

    protected fun cancelMy(buyer: User?, orderUuid: String, account: Map<String, Any?>? = null): ResultActionsDsl =
        v2Post(buyer, "/me/orders/$orderUuid/cancel", mapOf("refundAccount" to account))

    protected fun hostDetail(requester: User, eventId: Long, orderUuid: String): JsonNode =
        v2Get(requester, "/events/$eventId/orders/$orderUuid").andExpect { status { isOk() } }.data()

    /** 무료 티켓 (승인 여부 선택) */
    protected fun freeTicket(shop: Shop, approvalRequired: Boolean, supplyCount: Long = 10, name: String = "무료"): Long =
        createTicket(shop.team.manager, shop.eventId, freeBody(name = name, supplyCount = supplyCount, approvalRequired = approvalRequired))

    protected fun freeBodyOf(shop: Shop, ticketId: Long, quantity: Long = 1) =
        orderBody(shop.eventId, ticketId, quantity, method = "FREE", depositorName = null)

    protected fun setEventStart(eventId: Long, startAt: LocalDateTime) {
        val event = eventRepository.findById(eventId).get()
        ReflectionTestUtils.setField(event, "eventBasic", EventBasic(name = event.getEventName(), startAt = startAt, runTime = 120))
        eventRepository.save(event)
    }

    protected fun stock(ticketItemId: Long): Long = ticketItemRepository.findById(ticketItemId).get().quantity!!
}
