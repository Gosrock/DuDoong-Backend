package band.gosrock.api.v2.operation

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.operation.usecase.V2ReadIssuedTicketsUseCase
import band.gosrock.api.v2.operation.usecase.V2ReadOrdersUseCase
import band.gosrock.api.v2.order.V2UserOrderTestSupport
import jakarta.persistence.EntityManagerFactory
import org.apache.poi.ss.usermodel.Sheet
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc

/**
 * R-6 주문 엑셀 옵션 응답 컬럼 (#730). 옵션 컬럼은 I-3 발급 티켓 엑셀과 같은 규칙(답변에 나온 옵션 그룹 id 순, 이름이 겹치면 `이름(id)`).
 * 한 주문 = 한 행, 라인이 여러 개면 같은 응답끼리 수량을 더해 `응답 ×수량` 을 쉼표로 잇는다. H2 주의: 승인할 주문은 2장 이하
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 주문 엑셀 - 옵션 응답 (#730)")
class V2OrderExportOptionsTest : V2UserOrderTestSupport() {

    @Autowired private lateinit var entityManagerFactory: EntityManagerFactory

    private fun orderSheet(shop: Shop): Sheet = v2Get(shop.team.guest, "/events/${shop.eventId}/orders/export").andExpect { status { isOk() } }.sheet()

    private fun ticketSheet(shop: Shop): Sheet = v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets/export").andExpect { status { isOk() } }.sheet()

    /** 주문번호 → 옵션 헤더별 셀 */
    private fun Sheet.optionCells(headers: List<String>): Map<String, List<String>> =
        column("주문번호").mapIndexed { i, no -> no to headers.map { column(it)[i] } }.toMap()

    private fun orderNo(uuid: String): String = orderRepository.findByOrderUuid(uuid).get().orderNo!!

    @Test
    fun `옵션 헤더는 I-3 과 같다 (기본 열 뒤에 옵션 그룹 id 순)`() {
        val shop = Shop()
        shop.approved(newBuyer())
        v2OrderOk(newBuyer(), shopBody(shop, yes = false))
        val orderHeaders = orderSheet(shop).headers()
        val ticketHeaders = ticketSheet(shop).headers()
        assertEquals(V2ReadOrdersUseCase.ORDER_HEADERS, orderHeaders.take(V2ReadOrdersUseCase.ORDER_HEADERS.size))
        val orderOptions = orderHeaders.drop(V2ReadOrdersUseCase.ORDER_HEADERS.size)
        assertEquals(ticketHeaders.drop(V2ReadIssuedTicketsUseCase.TICKET_HEADERS.size), orderOptions)
        // 옵션 '입금자명' 은 R-6 기본 열 '입금자명' 과 겹쳐 id 를 붙인다 (I-3 도 같게)
        assertEquals(listOf("뒷풀이", "입금자명(${shop.subjectiveOptionId})"), orderOptions)
    }

    @Test
    fun `이름이 같은 옵션은 두 엑셀 모두 이름(id) 로 구분`() {
        val shop = Shop()
        val sameName = createOption(shop.team.manager, shop.eventId, type = "SUBJECTIVE", yesAdditionalPrice = null, name = "뒷풀이")
        val other = createTicket(shop.team.manager, shop.eventId, dudoongBody(name = "다른티켓", supplyCount = 10))
        putOptions(shop.team.manager, shop.eventId, other, listOf(sameName)).andExpect { status { isOk() } }
        shop.approved(newBuyer())
        v1Buy(newBuyer(), shop.team.master, shop.eventId, other, approval = true)
        val expected = listOf("뒷풀이(${shop.yesNoOptionId})", "입금자명(${shop.subjectiveOptionId})", "뒷풀이($sameName)")
        assertEquals(expected, orderSheet(shop).headers().drop(V2ReadOrdersUseCase.ORDER_HEADERS.size))
        assertEquals(expected, ticketSheet(shop).headers().drop(V2ReadIssuedTicketsUseCase.TICKET_HEADERS.size))
    }

    @Test
    fun `한 주문 한 행 - 라인 1개는 응답 그대로, 여러 라인은 응답 ×수량 (같은 응답 합산), 옵션 없는 주문은 빈 칸, 수식 방어`() {
        val shop = Shop()
        val single = v2OrderOk(newBuyer(), shopBody(shop, quantity = 3, yes = true)).at("/orderUuid").asText()
        val perTicket = v2OrderOk(
            newBuyer(),
            orderBody(
                shop.eventId, shop.ticketId, quantity = 3,
                perTicket = listOf(answers(shop, yes = true, text = "=SUM(1)"), answers(shop, yes = false, text = "=SUM(1)"), answers(shop, yes = true, text = "김")),
            ),
        ).at("/orderUuid").asText()
        val noOptions = createTicket(shop.team.manager, shop.eventId, dudoongBody(name = "옵션없음", supplyCount = 10))
        val plain = v2OrderOk(newBuyer(), orderBody(shop.eventId, noOptions)).at("/orderUuid").asText()

        val sheet = orderSheet(shop)
        assertEquals(3, sheet.lastRowNum, "주문 3건 = 3행 (라인 수와 무관)")
        val cells = sheet.optionCells(listOf("뒷풀이", "입금자명(${shop.subjectiveOptionId})"))
        assertEquals(listOf("예", "홍길동"), cells.getValue(orderNo(single)))
        assertEquals(listOf("예 ×2, 아니요 ×1", "'=SUM(1) ×2, 김 ×1"), cells.getValue(orderNo(perTicket)))
        assertEquals(listOf("", ""), cells.getValue(orderNo(plain)))
    }

    @Test
    fun `N+1 없음 - 주문·라인 수와 관계없이 쿼리 수가 같다 (라인 답변 일괄 적재, 옵션 이름 한 번)`() {
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        val shop = Shop()
        // 통계는 컨텍스트 전체 공유라 다른 테스트의 비동기 알림 저장이 섞일 수 있다 → 3번 재고 최솟값
        fun countQueries(): Long = (1..3).minOf {
            statistics.isStatisticsEnabled = true
            try {
                statistics.clear()
                orderSheet(shop)
                statistics.prepareStatementCount
            } finally {
                statistics.isStatisticsEnabled = false
            }
        }
        v2OrderOk(newBuyer(), shopBody(shop))
        val one = countQueries()
        repeat(4) { i ->
            v2OrderOk(
                newBuyer(),
                orderBody(shop.eventId, shop.ticketId, quantity = 2, perTicket = listOf(answers(shop, yes = true, text = "a$i"), answers(shop, yes = false, text = "b$i"))),
            )
        }
        assertEquals(one, countQueries(), "주문 1건: $one")
    }
}
