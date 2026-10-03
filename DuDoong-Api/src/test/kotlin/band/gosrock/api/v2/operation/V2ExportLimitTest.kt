package band.gosrock.api.v2.operation

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.context.TestPropertySource

/**
 * v2 엑셀 행 상한 (#712). 운영 상한은 10,000 (`V2OrderQuery.EXPORT_MAX_ROWS`). 1만 행을 만들지 않도록 상한만 2로 낮춰 같은 판정 경로를 검증한다
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = ["v2.export.max-rows=2"])
@DisplayName("v2 엑셀 행 상한")
class V2ExportLimitTest : V2OperationTestSupport() {

    @Test
    fun `상한 초과면 400 (주문 Order_400_19, 발급 티켓 IssuedTicket_400_7), 필터로 줄이면 성공`() {
        val shop = Shop()
        val approved = shop.approved(newBuyer("승인1"), quantity = 2)
        shop.approved(newBuyer("승인2"))
        shop.order(newBuyer("대기"))

        assertEquals("Order_400_19", v2Get(shop.team.guest, "/events/${shop.eventId}/orders/export").andExpect { status { isBadRequest() } }.code())
        val filtered = v2Get(shop.team.guest, "/events/${shop.eventId}/orders/export", mapOf("status" to "APPROVED")).andExpect { status { isOk() } }.sheet()
        assertEquals(2, filtered.lastRowNum)

        assertEquals("IssuedTicket_400_7", v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets/export").andExpect { status { isBadRequest() } }.code())
        checkIn(shop.team.guest, shop.eventId, shop.ticketUuids(approved)[0]).andExpect { status { isOk() } }
        val done = v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets/export", mapOf("entrance" to "DONE")).andExpect { status { isOk() } }.sheet()
        assertEquals(1, done.lastRowNum)
        // 목록 API 는 상한과 무관
        assertEquals(3, orders(shop.team.guest, shop.eventId).at("/counts/all").asLong())
    }
}
