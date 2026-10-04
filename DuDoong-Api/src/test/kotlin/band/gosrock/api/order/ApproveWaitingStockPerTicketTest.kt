package band.gosrock.api.order

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.order.V2UserOrderTestSupport
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.user.domain.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.ResultActionsDsl

/**
 * 승인형 주문 생성 재고 검사는 **같은 티켓**의 승인 대기 수량만 더한다 (#720).
 * 예전에는 공연 전체 승인 대기 수량을 이 티켓 재고와 비교해, 다른 티켓 대기가 쌓이면 재고가 남은 티켓도 매진(Ticket_Item_400_1)으로 거절했다.
 * v1 주문 API(카트 → 주문)와 v2 O-1 이 같은 검증([band.gosrock.domain.domains.order.domain.Order.createApproveOrder])을 쓰므로 두 경로를 모두 본다.
 * H2 주의: v1 승인은 발급(별도 REQUIRES_NEW 커밋, 재고 감소) 뒤에 재고·매수 제한을 다시 보는데, H2(READ COMMITTED)는 방금 줄어든 재고를 본다
 * (MySQL REPEATABLE READ 는 승인 트랜잭션 스냅샷이라 줄기 전 값). 그래서 H2 에서는 남은 재고의 절반 이하 주문만 승인한다 — 경계는 MySQL E2E(test_46)
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("승인형 주문 재고 검사 - 티켓별 승인 대기 수량 (#720)")
class ApproveWaitingStockPerTicketTest : V2UserOrderTestSupport() {

    enum class Path { V1, V2 }

    /** 승인형(두둥) 티켓 2종(A·B, 옵션 없음)이 있는 OPEN 공연 */
    private inner class TwoTickets(supplyA: Long, supplyB: Long = 20) {
        val team = Team()
        val eventId get() = team.eventId
        val a = createTicket(team.manager, team.eventId, dudoongBody(name = "A석", supplyCount = supplyA))
        val b = createTicket(team.manager, team.eventId, dudoongBody(name = "B석", supplyCount = supplyB))

        init {
            setEventStatus(team.eventId, EventStatus.OPEN)
        }

        /** 승인 대기 주문 (v1 카트 → 주문) */
        fun pending(ticketId: Long, quantity: Long): String = v1Order(newBuyer(), eventId, ticketId, quantity)

        fun place(path: Path, ticketId: Long, quantity: Long, buyer: User = newBuyer()): ResultActionsDsl = when (path) {
            Path.V1 -> {
                val cartId = v1Cart(buyer, ticketId, quantity).andExpect { status { isOk() } }.data().at("/cartId").asLong()
                v1CreateOrder(buyer, cartId)
            }
            Path.V2 -> v2CreateOrder(buyer, orderBody(eventId, ticketId, quantity))
        }

        fun placeOk(path: Path, ticketId: Long, quantity: Long): String {
            val result = place(path, ticketId, quantity)
            assertEquals(200, result.andReturn().response.status, result.body().toString())
            return result.data().at(if (path == Path.V1) "/orderId" else "/orderUuid").asText()
        }

        fun placeRejected(path: Path, ticketId: Long, quantity: Long) {
            assertEquals(QUANTITY_LACK, place(path, ticketId, quantity).andExpect { status { isBadRequest() } }.code())
        }

        fun approve(path: Path, orderUuid: String): ResultActionsDsl = when (path) {
            Path.V1 -> v1Approve(team.master, eventId, orderUuid)
            Path.V2 -> v2Post(team.manager, "/events/$eventId/orders/$orderUuid/approve")
        }

        fun approveOk(path: Path, orderUuid: String) {
            val result = approve(path, orderUuid)
            assertEquals(200, result.andReturn().response.status, result.body().toString())
        }

        fun issuedCount(ticketId: Long): Int = issuedTicketRepository.findAll().count { it.itemInfo?.ticketItemId == ticketId }
    }

    private fun statusOf(orderUuid: String): OrderStatus = orderRepository.findByOrderUuid(orderUuid).get().orderStatus

    @ParameterizedTest
    @EnumSource(Path::class)
    fun `(a) 다른 티켓 승인 대기가 이 티켓 재고보다 많아도 이 티켓 주문은 성공`(path: Path) {
        val shop = TwoTickets(supplyA = 2)
        // B 승인 대기 4장 (> A 재고 2)
        shop.pending(shop.b, 2)
        shop.pending(shop.b, 2)

        val uuid = shop.placeOk(path, shop.a, 2)
        assertEquals(OrderStatus.PENDING_APPROVE, statusOf(uuid))
    }

    @ParameterizedTest
    @EnumSource(Path::class)
    fun `(b) 같은 티켓 승인 대기 + 이번 주문이 재고를 넘으면 거절 (Ticket_Item_400_1)`(path: Path) {
        val shop = TwoTickets(supplyA = 3)
        shop.pending(shop.a, 2)

        shop.placeRejected(path, shop.a, 2)
        // 다른 티켓(B)은 영향 없음
        shop.placeOk(path, shop.b, 2)
    }

    @ParameterizedTest
    @EnumSource(Path::class)
    fun `(c) 같은 티켓 승인 대기 + 이번 주문이 재고와 같으면 성공 (경계), 다른 티켓 대기는 세지 않는다`(path: Path) {
        val shop = TwoTickets(supplyA = 3)
        shop.pending(shop.a, 2)
        shop.pending(shop.b, 2)

        shop.placeOk(path, shop.a, 1)
        // 이제 A 는 대기 3 = 재고 3 → 1장 더는 거절
        shop.placeRejected(path, shop.a, 1)
    }

    @ParameterizedTest
    @EnumSource(Path::class)
    fun `(d) 승인 대기 → 승인 시 재검사로 초과 판매 없음`(path: Path) {
        val shop = TwoTickets(supplyA = 4)
        val first = shop.pending(shop.a, 2)
        val second = shop.placeOk(path, shop.a, 1)
        shop.placeRejected(path, shop.a, 2)

        // 승인하면 재고가 줄고 대기에서 빠진다: 재고 2, 대기 1 → 2장은 여전히 거절
        shop.approveOk(path, first)
        assertEquals(2, stock(shop.a))
        shop.placeRejected(path, shop.a, 2)
        shop.approveOk(path, second)
        assertEquals(1, stock(shop.a))
        // 마지막 1장: 대기 1 + 발급 3 = 공급 4, 그 이상은 거절
        val third = shop.placeOk(path, shop.a, 1)
        shop.placeRejected(path, shop.a, 1)
        assertEquals(3, shop.issuedCount(shop.a))
        assertEquals(OrderStatus.PENDING_APPROVE, statusOf(third))

        // 승인 시 재검사(validCanDone): 대기 주문이 있는데 재고가 다른 이유로 줄었으면 승인이 거절되고 대기 그대로
        val late = shop.pending(shop.b, 2)
        val item = ticketItemRepository.findById(shop.b).get()
        ReflectionTestUtils.setField(item, "quantity", 1L)
        ticketItemRepository.save(item)
        assertEquals(QUANTITY_LACK, shop.approve(path, late).andExpect { status { isBadRequest() } }.code())
        assertEquals(OrderStatus.PENDING_APPROVE, statusOf(late))
        assertEquals(1, stock(shop.b))
        assertEquals(0, shop.issuedCount(shop.b))
    }

    companion object {
        private const val QUANTITY_LACK = "Ticket_Item_400_1"
    }
}
