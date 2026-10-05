package band.gosrock.api.v2.operation

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.operation.usecase.V2ExcelHeaders
import band.gosrock.api.v2.order.V2UserOrderTestSupport
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupStatus
import band.gosrock.domain.domains.ticket_item.repository.OptionGroupRepository
import jakarta.persistence.EntityManagerFactory
import org.apache.poi.ss.usermodel.Sheet
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.util.ReflectionTestUtils

/**
 * R-6 주문 엑셀 옵션 응답 컬럼 (#730). 옵션 컬럼은 I-3 발급 티켓 엑셀과 같은 규칙(답변에 나온 옵션 그룹 id 순, 이름이 다른 옵션·그 엑셀 기본 열과 겹치면 `이름(id)`).
 * 열 집합은 엑셀마다 대상·필터가 달라 다를 수 있다. 한 주문 = 한 행, 라인이 여러 개면 같은 응답끼리 수량을 더해 `응답 ×수량` 을 줄바꿈으로 잇는다. H2 주의: 승인할 주문은 2장 이하
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 주문 엑셀 - 옵션 응답 (#730)")
class V2OrderExportOptionsTest : V2UserOrderTestSupport() {

    @Autowired private lateinit var entityManagerFactory: EntityManagerFactory

    @Autowired private lateinit var optionGroupRepository: OptionGroupRepository

    private fun orderSheet(shop: Shop): Sheet = v2Get(shop.team.guest, "/events/${shop.eventId}/orders/export").andExpect { status { isOk() } }.sheet()

    private fun ticketSheet(shop: Shop): Sheet = v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets/export").andExpect { status { isOk() } }.sheet()

    /** 주문번호 → 옵션 헤더별 셀 */
    private fun Sheet.optionCells(headers: List<String>): Map<String, List<String>> =
        column("주문번호").mapIndexed { i, no -> no to headers.map { column(it)[i] } }.toMap()

    private fun orderNo(uuid: String): String = orderRepository.findByOrderUuid(uuid).get().orderNo!!

    private val orderBase = V2ExcelHeaders.ORDER.size
    private val ticketBase = V2ExcelHeaders.ISSUED_TICKET.size

    @Test
    fun `옵션 헤더는 I-3 과 같은 규칙 - 각 엑셀은 자기 기본 열과만 이름 충돌을 본다`() {
        val shop = Shop()
        shop.approved(newBuyer())
        v2OrderOk(newBuyer(), shopBody(shop, yes = false))
        val orderHeaders = orderSheet(shop).headers()
        val ticketHeaders = ticketSheet(shop).headers()
        assertEquals(V2ExcelHeaders.ORDER, orderHeaders.take(orderBase))
        assertEquals(V2ExcelHeaders.ISSUED_TICKET, ticketHeaders.take(ticketBase))
        // 옵션 '입금자명' 은 R-6 기본 열 '입금자명' 과만 겹친다 → R-6 만 id 를 붙이고, I-3 은 기존 그대로
        assertEquals(listOf("뒷풀이", "입금자명(${shop.subjectiveOptionId})"), orderHeaders.drop(orderBase))
        assertEquals(listOf("뒷풀이", "입금자명"), ticketHeaders.drop(ticketBase))
    }

    @Test
    fun `이름이 같은 옵션은 두 엑셀 모두 이름(id), 그래도 겹치면 (id) 를 한 번 더 붙인다`() {
        val shop = Shop()
        val sameName = createOption(shop.team.manager, shop.eventId, type = "SUBJECTIVE", yesAdditionalPrice = null, name = "뒷풀이")
        // 질문 이름이 원래 '뒷풀이(첫 옵션 id)' 인 옵션 → 첫 옵션의 '뒷풀이(id)' 와 겹친다
        val lookAlike = createOption(shop.team.manager, shop.eventId, type = "SUBJECTIVE", yesAdditionalPrice = null, name = "뒷풀이(${shop.yesNoOptionId})")
        val other = createTicket(shop.team.manager, shop.eventId, dudoongBody(name = "다른티켓", supplyCount = 10))
        putOptions(shop.team.manager, shop.eventId, other, listOf(sameName, lookAlike)).andExpect { status { isOk() } }
        shop.approved(newBuyer())
        v1Buy(newBuyer(), shop.team.master, shop.eventId, other, approval = true)
        val y = shop.yesNoOptionId
        assertEquals(
            listOf("뒷풀이($y)", "입금자명(${shop.subjectiveOptionId})", "뒷풀이($sameName)", "뒷풀이($y)($lookAlike)"),
            orderSheet(shop).headers().drop(orderBase),
        )
        val ticketOptions = ticketSheet(shop).headers().drop(ticketBase)
        assertEquals(listOf("뒷풀이($y)", "입금자명", "뒷풀이($sameName)", "뒷풀이($y)($lookAlike)"), ticketOptions)
        assertEquals(ticketOptions.size, ticketOptions.toSet().size)
    }

    @Test
    fun `한 주문 한 행 - 라인 1개는 응답 그대로, 여러 라인은 응답 ×수량 을 줄바꿈으로 (같은 응답 합산, 자동 줄바꿈), 옵션 없는 주문은 빈 칸, 수식 방어`() {
        val shop = Shop()
        val single = v2OrderOk(newBuyer(), shopBody(shop, quantity = 3, yes = true)).at("/orderUuid").asText()
        val singleFormula = v2OrderOk(newBuyer(), orderBody(shop.eventId, shop.ticketId, answers = answers(shop, yes = false, text = "=SUM(2)"))).at("/orderUuid").asText()
        val perTicket = v2OrderOk(
            newBuyer(),
            orderBody(
                shop.eventId, shop.ticketId, quantity = 3,
                perTicket = listOf(answers(shop, yes = true, text = "=SUM(1)"), answers(shop, yes = false, text = "=SUM(1)"), answers(shop, yes = true, text = "김, 이")),
            ),
        ).at("/orderUuid").asText()
        val multiLine = v2OrderOk(newBuyer(), orderBody(shop.eventId, shop.ticketId, answers = answers(shop, text = "첫줄\n둘째줄"))).at("/orderUuid").asText()
        val noOptions = createTicket(shop.team.manager, shop.eventId, dudoongBody(name = "옵션없음", supplyCount = 10))
        val plain = v2OrderOk(newBuyer(), orderBody(shop.eventId, noOptions)).at("/orderUuid").asText()

        val sheet = orderSheet(shop)
        assertEquals(5, sheet.lastRowNum, "주문 5건 = 5행 (라인 수와 무관)")
        val headers = listOf("뒷풀이", "입금자명(${shop.subjectiveOptionId})")
        val cells = sheet.optionCells(headers)
        assertEquals(listOf("예", "홍길동"), cells.getValue(orderNo(single)))
        assertEquals(listOf("아니요", "'=SUM(2)"), cells.getValue(orderNo(singleFormula)), "라인 1개도 수식 방어")
        // 응답 안의 쉼표와 구분되도록 응답끼리는 줄바꿈
        assertEquals(listOf("예 ×2\n아니요 ×1", "'=SUM(1) ×2\n김, 이 ×1"), cells.getValue(orderNo(perTicket)))
        assertEquals(listOf("예", "첫줄 둘째줄"), cells.getValue(orderNo(multiLine)), "응답 안의 줄바꿈은 공백")
        assertEquals(listOf("", ""), cells.getValue(orderNo(plain)))

        val row = sheet.getRow(sheet.column("주문번호").indexOf(orderNo(perTicket)) + 1)
        headers.forEach { assertTrue(row.getCell(sheet.headers().indexOf(it)).cellStyle.wrapText, "$it 셀 자동 줄바꿈") }
        val singleRow = sheet.getRow(sheet.column("주문번호").indexOf(orderNo(single)) + 1)
        assertFalse(singleRow.getCell(sheet.headers().indexOf("뒷풀이")).cellStyle.wrapText)
    }

    @Test
    fun `수식으로 시작하는 옵션 이름은 헤더도 작은따옴표 (두 엑셀)`() {
        val shop = Shop()
        val evil = createOption(shop.team.manager, shop.eventId, type = "SUBJECTIVE", yesAdditionalPrice = null, name = "=cmd")
        val other = createTicket(shop.team.manager, shop.eventId, dudoongBody(name = "다른티켓", supplyCount = 10))
        putOptions(shop.team.manager, shop.eventId, other, listOf(evil)).andExpect { status { isOk() } }
        v1Buy(newBuyer(), shop.team.master, shop.eventId, other, approval = true)
        assertEquals(listOf("'=cmd"), orderSheet(shop).headers().drop(orderBase))
        assertEquals(listOf("'=cmd"), ticketSheet(shop).headers().drop(ticketBase))
    }

    @Test
    fun `옵션을 soft delete 해도 지난 답변 열은 남는다 (답변에 나온 옵션 기준)`() {
        val shop = Shop()
        val order = v2OrderOk(newBuyer(), shopBody(shop)).at("/orderUuid").asText()
        optionGroupRepository.findById(shop.yesNoOptionId).get().let {
            ReflectionTestUtils.setField(it, "optionGroupStatus", OptionGroupStatus.DELETED)
            optionGroupRepository.save(it)
        }
        val sheet = orderSheet(shop)
        assertEquals(listOf("뒷풀이", "입금자명(${shop.subjectiveOptionId})"), sheet.headers().drop(orderBase))
        assertEquals(listOf("예", "홍길동"), sheet.optionCells(listOf("뒷풀이", "입금자명(${shop.subjectiveOptionId})")).getValue(orderNo(order)))
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
