package band.gosrock.api.v2

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.operation.V2OperationTestSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

/** v2 컨벤션 통일 (#755): 바뀐 경로·이름, 목록 필터 ALL, 검색어 길이 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 컨벤션 (#755)")
class V2ConventionTest : V2OperationTestSupport() {

    @Test
    fun `옛 경로는 없다 - POST orders(→ me orders), master-transfer(→ transfer-master)`() {
        val shop = Shop()
        val buyer = newBuyer()
        val body = mapOf(
            "eventId" to shop.eventId, "ticketItemId" to shop.ticketId, "quantity" to 1,
            "options" to mapOf("applyToAll" to true, "answers" to emptyList<Any>()), "paymentChannel" to "BANK_TRANSFER",
            "depositorName" to "입금자", "agreeRefundPolicy" to true,
        )
        val old = mockMvc.post("/api/v2/orders") {
            with(auth(buyer))
            contentType = MediaType.APPLICATION_JSON
            content = json(body)
        }.andReturn().response.status
        assertTrue(old == 404 || old == 405, "옛 주문 생성 경로 status=$old")
        val oldTransfer = mockMvc.post("/api/v2/hosts/${shop.team.hostId}/master-transfer") {
            with(auth(shop.team.master))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("userId" to shop.team.manager.id))
        }.andReturn().response.status
        assertTrue(oldTransfer == 404 || oldTransfer == 405, "옛 마스터 양도 경로 status=$oldTransfer")
    }

    @Test
    fun `F-1 status 기본값 ALL - 생략·ALL 이 같은 결과, 없는 값은 400`() {
        val shop = Shop()
        val refused = shop.order(newBuyer())
        refuse(shop.team.manager, shop.eventId, refused, "SOLD_OUT").andExpect { status { isOk() } }
        val omitted = v2Get(shop.team.guest, "/events/${shop.eventId}/refunds").andExpect { status { isOk() } }.data()
        val all = v2Get(shop.team.guest, "/events/${shop.eventId}/refunds", mapOf("status" to "ALL")).andExpect { status { isOk() } }.data()
        assertEquals(1, omitted.at("/totalElements").asLong())
        assertEquals(omitted, all)
        assertEquals(0, v2Get(shop.team.guest, "/events/${shop.eventId}/refunds", mapOf("status" to "COMPLETED")).andExpect { status { isOk() } }.data().at("/totalElements").asLong())
        v2Get(shop.team.guest, "/events/${shop.eventId}/refunds", mapOf("status" to "NONE")).andExpect { status { isBadRequest() } }
    }

    @Test
    fun `검색어는 모든 목록에서 50자까지 (51자는 400)`() {
        val shop = Shop()
        val ok = "가".repeat(50)
        val tooLong = "가".repeat(51)
        val paths = listOf(
            shop.team.guest to "/events/${shop.eventId}/orders",
            shop.team.guest to "/events/${shop.eventId}/orders/export",
            shop.team.guest to "/events/${shop.eventId}/issued-tickets",
            shop.team.guest to "/events/${shop.eventId}/issued-tickets/export",
            shop.team.guest to "/me/events",
            shop.team.guest to "/me/hosts",
            shop.team.guest to "/events",
        )
        paths.forEach { (who, path) ->
            v2Get(who, path, mapOf("keyword" to ok)).andExpect { status { isOk() } }
            v2Get(who, path, mapOf("keyword" to tooLong)).andExpect { status { isBadRequest() } }
        }
    }
}
