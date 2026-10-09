package band.gosrock.api.order

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.operation.V2OperationTestSupport
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.user.domain.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

/**
 * v1 호스트 주문·환불 API 의 공연 소속 검증 (#760).
 * 경로 공연의 권한이 있어도 다른 공연의 주문이면 404 이고 주문 상태는 그대로다. 같은 공연 주문은 기존처럼 처리된다
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v1 호스트 주문 - 공연 소속 검증 (#760)")
class V1OrderEventScopeTest : V2OperationTestSupport() {

    private fun v1(method: String, user: User, path: String, body: Any? = null): ResultActionsDsl {
        val url = "/api/v1/events$path"
        return when (method) {
            "GET" -> mockMvc.get(url) { with(auth(user)) }
            "PATCH" -> mockMvc.patch(url) { with(auth(user)) }
            else -> mockMvc.post(url) {
                with(auth(user))
                if (body != null) {
                    contentType = MediaType.APPLICATION_JSON
                    content = json(body)
                }
            }
        }
    }

    private fun order(orderUuid: String): Order = orderRepository.findByOrderUuid(orderUuid).get()

    private fun ResultActionsDsl.expectOrderNotFound() =
        assertEquals("Order_404_1", andExpect { status { isNotFound() } }.code())

    @Test
    fun `승인 - 다른 공연 주문이면 404, 상태·발급 티켓 그대로`() {
        val mine = Shop()
        val other = Shop("다른호스트")
        val target = other.order(newBuyer())
        val manager = mine.team.manager

        v1("POST", manager, "/${mine.eventId}/orders/$target/approve").expectOrderNotFound()
        assertEquals(OrderStatus.PENDING_APPROVE, order(target).orderStatus)
        assertEquals(0, issuedTicketRepository.findAllByOrderUuid(target).size)

        // 같은 공연 주문은 기존대로 승인된다
        val own = mine.order(newBuyer())
        v1("POST", manager, "/${mine.eventId}/orders/$own/approve").andExpect { status { isOk() } }
        assertEquals(OrderStatus.APPROVED, order(own).orderStatus)
    }

    @Test
    fun `거절 - 다른 공연 주문이면 404, 상태 그대로`() {
        val mine = Shop()
        val other = Shop("다른호스트")
        val target = other.order(newBuyer())

        v1("POST", mine.team.manager, "/${mine.eventId}/orders/$target/refuse", mapOf("reason" to "사유")).expectOrderNotFound()
        assertEquals(OrderStatus.PENDING_APPROVE, order(target).orderStatus)
        assertEquals(RefundStatus.NONE, order(target).refundStatus)
    }

    @Test
    fun `취소 - 다른 공연 주문이면 404, 상태·발급 티켓 그대로`() {
        val mine = Shop()
        val other = Shop("다른호스트")
        val target = other.approved(newBuyer())
        val before = order(target).orderStatus
        val tickets = issuedTicketRepository.findAllByOrderUuid(target).map { it.issuedTicketStatus }

        v1("POST", mine.team.manager, "/${mine.eventId}/orders/$target/cancel", mapOf("reason" to "사유")).expectOrderNotFound()
        assertEquals(before, order(target).orderStatus)
        assertEquals(tickets, issuedTicketRepository.findAllByOrderUuid(target).map { it.issuedTicketStatus })
    }

    @Test
    fun `환불 완료 - 다른 공연 주문이면 404, 환불 상태 그대로`() {
        val mine = Shop()
        val other = Shop("다른호스트")
        val target = other.order(newBuyer())
        refuse(other.team.manager, other.eventId, target, "SOLD_OUT").andExpect { status { isOk() } }
        assertEquals(RefundStatus.REFUND_REQUESTED, order(target).refundStatus)

        v1("PATCH", mine.team.manager, "/${mine.eventId}/refunds/$target/complete").expectOrderNotFound()
        assertEquals(RefundStatus.REFUND_REQUESTED, order(target).refundStatus)

        // 같은 공연 경로로는 기존대로 완료된다
        v1("PATCH", other.team.manager, "/${other.eventId}/refunds/$target/complete").andExpect { status { isOk() } }
        assertEquals(RefundStatus.REFUND_COMPLETED, order(target).refundStatus)
    }

    @Test
    fun `주문 상세 - 다른 공연 주문이면 404`() {
        val mine = Shop()
        val other = Shop("다른호스트")
        val target = other.order(newBuyer())

        v1("GET", mine.team.guest, "/${mine.eventId}/orders/$target").expectOrderNotFound()
        v1("GET", other.team.guest, "/${other.eventId}/orders/$target").andExpect { status { isOk() } }
    }

    @Test
    fun `환불 상세 - 다른 공연 주문이면 404`() {
        val mine = Shop()
        val other = Shop("다른호스트")
        val target = other.order(newBuyer())
        refuse(other.team.manager, other.eventId, target, "SOLD_OUT").andExpect { status { isOk() } }

        v1("GET", mine.team.guest, "/${mine.eventId}/refunds/$target").expectOrderNotFound()
        v1("GET", other.team.guest, "/${other.eventId}/refunds/$target").andExpect { status { isOk() } }
    }
}
