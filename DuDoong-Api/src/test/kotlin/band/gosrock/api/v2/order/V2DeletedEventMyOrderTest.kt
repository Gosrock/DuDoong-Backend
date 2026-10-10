package band.gosrock.api.v2.order

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.user.domain.User
import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.get

/**
 * 공연이 삭제(@Where)된 뒤 내 주문·티켓 (#788).
 * v2 상세는 목록과 같이 event = null 로 200, v1 최근 주문은 삭제 공연 주문을 건너뛴다 (v1 목록과 같은 기준)
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("삭제된 공연의 내 주문·티켓 (#788)")
class V2DeletedEventMyOrderTest : V2UserOrderTestSupport() {

    private fun myTicket(user: User, ticketUuid: String): JsonNode =
        v2Get(user, "/me/tickets/$ticketUuid").andExpect { status { isOk() } }.data()

    private fun v1Recent(user: User): JsonNode =
        mockMvc.get("/api/v1/orders/recent") { with(auth(user)) }.andExpect { status { isOk() } }.data()

    @Test
    fun `v2 주문 상세 - 공연이 삭제되면 목록과 같이 event null 로 200, 취소 불가, 발급 티켓은 그대로`() {
        val shop = Shop()
        val buyer = newBuyer()
        val orderUuid = shop.approved(buyer)
        assertFalse(myOrder(buyer, orderUuid).andExpect { status { isOk() } }.data().at("/event").isNull)

        setEventStatus(shop.eventId, EventStatus.DELETED)

        val listed = myOrders(buyer).at("/content").single { it.at("/orderUuid").asText() == orderUuid }
        assertTrue(listed.at("/event").isNull)
        val detail = myOrder(buyer, orderUuid).andExpect { status { isOk() } }.data()
        assertEquals(orderUuid, detail.at("/orderUuid").asText())
        assertTrue(detail.at("/event").isNull)
        assertFalse(detail.at("/canCancel").asBoolean())
        assertEquals(1, detail.at("/issuedTickets").size())
        // 남의 주문은 여전히 404
        myOrder(newBuyer(), orderUuid).andExpect { status { isNotFound() } }
    }

    @Test
    fun `v2 티켓 상세 - 공연이 삭제되면 목록과 같이 event null 로 200, QR·선물·반환 비활성`() {
        val shop = Shop()
        val buyer = newBuyer()
        val ticketUuid = shop.ticketUuids(shop.approved(buyer)).single()
        val before = myTicket(buyer, ticketUuid)
        assertEquals(ticketUuid, before.at("/qrValue").asText())
        assertTrue(before.at("/canGift").asBoolean())

        setEventStatus(shop.eventId, EventStatus.DELETED)

        val group = v2Get(buyer, "/me/tickets").andExpect { status { isOk() } }.data().at("/groups")
            .single { g -> g.at("/tickets").any { it.at("/ticketUuid").asText() == ticketUuid } }
        assertTrue(group.at("/event").isNull)
        val detail = myTicket(buyer, ticketUuid)
        assertEquals(ticketUuid, detail.at("/ticketUuid").asText())
        assertTrue(detail.at("/event").isNull)
        assertTrue(detail.at("/qrValue").isNull)
        assertFalse(detail.at("/canGift").asBoolean())
        assertFalse(detail.at("/canReturn").asBoolean())
        assertFalse(detail.at("/isGiftExpired").asBoolean())
        // 남의 티켓은 여전히 404
        v2Get(newBuyer(), "/me/tickets/$ticketUuid").andExpect { status { isNotFound() } }
    }

    @Test
    fun `v1 최근 주문 - 가장 최근 주문의 공연이 삭제되면 그 주문을 건너뛰고, 남은 게 없으면 최근 주문 없음`() {
        val buyer = newBuyer()
        val older = Shop()
        val olderOrder = older.order(buyer)
        val newer = Shop()
        val newerOrder = newer.order(buyer)
        assertEquals(newerOrder, v1Recent(buyer).at("/orderUuid").asText())

        setEventStatus(newer.eventId, EventStatus.DELETED)
        assertEquals(olderOrder, v1Recent(buyer).at("/orderUuid").asText())

        setEventStatus(older.eventId, EventStatus.DELETED)
        val none = v1Recent(buyer)
        assertTrue(none.isNull || none.isMissingNode)
    }
}
