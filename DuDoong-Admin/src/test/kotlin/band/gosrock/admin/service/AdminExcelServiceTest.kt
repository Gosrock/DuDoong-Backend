package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminIssuedTicketResponse
import band.gosrock.admin.model.dto.response.AdminOrderResponse
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.ticket_item.domain.Option
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.io.ByteArrayInputStream
import java.time.LocalDateTime

@DisplayName("AdminExcelService")
class AdminExcelServiceTest {

    private lateinit var adminExcelService: AdminExcelService

    @BeforeEach
    fun setUp() {
        adminExcelService = AdminExcelService()
    }

    @Nested
    @DisplayName("generateOrdersExcel")
    inner class GenerateOrdersExcelTest {

        @Test
        @DisplayName("주문 엑셀 헤더 컬럼이 올바르게 생성된다")
        fun headerColumnsAreCorrect() {
            val bytes = adminExcelService.generateOrdersExcel(emptyList())
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)
            val headerRow = sheet.getRow(0)

            val expectedHeaders = listOf(
                "주문번호", "주문번호(읽기용)", "사용자", "이벤트", "티켓", "금액", "상태",
                "주문방식", "결제수단", "승인일시", "취소일시", "할인금액", "쿠폰명", "주문일",
            )
            expectedHeaders.forEachIndexed { i, expected ->
                assertEquals(expected, headerRow.getCell(i).stringCellValue, "헤더[$i]")
            }
            assertEquals(expectedHeaders.size, headerRow.lastCellNum.toInt())
            workbook.close()
        }

        @Test
        @DisplayName("주문 데이터 행이 올바르게 생성된다")
        fun dataRowsAreCorrect() {
            val now = LocalDateTime.of(2026, 3, 22, 10, 0)
            val orders = listOf(
                AdminOrderResponse(
                    orderId = "uuid-1",
                    orderNo = "R100001",
                    userName = "테스트유저",
                    eventName = "테스트이벤트",
                    ticketName = "일반 티켓",
                    totalAmount = 10000L,
                    orderStatus = OrderStatus.CONFIRM,
                    createdAt = now,
                    orderMethod = "PAYMENT",
                    userId = 1L,
                    eventId = 1L,
                    approvedAt = now.plusMinutes(5),
                    withDrawAt = null,
                    paymentMethod = "CARD",
                    receiptUrl = "https://example.com/receipt",
                    supplyAmount = "10000",
                    discountAmount = "1000",
                    couponName = "첫주문쿠폰",
                ),
            )

            val bytes = adminExcelService.generateOrdersExcel(orders)
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)
            val dataRow = sheet.getRow(1)

            assertEquals("uuid-1", dataRow.getCell(0).stringCellValue)
            assertEquals("R100001", dataRow.getCell(1).stringCellValue)
            assertEquals("테스트유저", dataRow.getCell(2).stringCellValue)
            assertEquals("테스트이벤트", dataRow.getCell(3).stringCellValue)
            assertEquals("일반 티켓", dataRow.getCell(4).stringCellValue)
            assertEquals(10000.0, dataRow.getCell(5).numericCellValue)
            assertEquals("CONFIRM", dataRow.getCell(6).stringCellValue)
            assertEquals("PAYMENT", dataRow.getCell(7).stringCellValue)
            assertEquals("CARD", dataRow.getCell(8).stringCellValue)
            assertEquals(now.plusMinutes(5).toString(), dataRow.getCell(9).stringCellValue)
            assertEquals("", dataRow.getCell(10).stringCellValue) // withDrawAt null
            assertEquals("1000", dataRow.getCell(11).stringCellValue)
            assertEquals("첫주문쿠폰", dataRow.getCell(12).stringCellValue)
            assertEquals(now.toString(), dataRow.getCell(13).stringCellValue)
            workbook.close()
        }
    }

    @Nested
    @DisplayName("generateIssuedTicketsExcel")
    inner class GenerateIssuedTicketsExcelTest {

        @Test
        @DisplayName("기본 발급 티켓 엑셀 헤더가 올바르게 생성된다")
        fun basicHeaderColumnsAreCorrect() {
            val bytes = adminExcelService.generateIssuedTicketsExcel(emptyList())
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)
            val headerRow = sheet.getRow(0)

            val expectedHeaders = listOf("티켓번호", "유저명", "티켓종류", "주문번호", "입장여부", "발급일")
            expectedHeaders.forEachIndexed { i, expected ->
                assertEquals(expected, headerRow.getCell(i).stringCellValue, "헤더[$i]")
            }
            assertEquals(expectedHeaders.size, headerRow.lastCellNum.toInt())
            workbook.close()
        }
    }

    @Nested
    @DisplayName("generateIssuedTicketsExcelWithOptions")
    inner class GenerateIssuedTicketsExcelWithOptionsTest {

        @Test
        @DisplayName("옵션 동적 컬럼이 헤더에 추가된다")
        fun optionDynamicHeadersAreAdded() {
            val optionGroup = createOptionGroup(1L, "이름 수집용", OptionGroupType.SUBJECTIVE)
            val option1 = createOption(10L, "참석자 이름", optionGroup)
            val option2 = createOption(11L, "연락처", optionGroup)

            val bytes = adminExcelService.generateIssuedTicketsExcelWithOptions(
                tickets = emptyList(),
                options = listOf(option1, option2),
                issuedTickets = emptyList(),
            )
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)
            val headerRow = sheet.getRow(0)

            // 기본 6 컬럼 + 옵션 2 컬럼
            assertEquals(8, headerRow.lastCellNum.toInt())
            assertEquals("참석자 이름", headerRow.getCell(6).stringCellValue)
            assertEquals("연락처", headerRow.getCell(7).stringCellValue)
            workbook.close()
        }

        @Test
        @DisplayName("옵션 응답이 올바르게 매핑된다")
        fun optionAnswersAreMapped() {
            val optionGroup = createOptionGroup(1L, "참석자 정보", OptionGroupType.SUBJECTIVE)
            val option1 = createOption(10L, "참석자 이름", optionGroup)
            val option2 = createOption(11L, "연락처", optionGroup)

            val now = LocalDateTime.of(2026, 3, 22, 10, 0)

            // IssuedTicket 엔티티 생성 (옵션 응답 포함)
            val answer1 = IssuedTicketOptionAnswer(optionId = 10L, answer = "홍길동")
            val answer2 = IssuedTicketOptionAnswer(optionId = 11L, answer = "010-1234-5678")
            val issuedTicket = createIssuedTicket(100L, listOf(answer1, answer2))

            val ticketResponse = AdminIssuedTicketResponse(
                id = 100L,
                issuedTicketNo = "T100001",
                userName = "테스트유저",
                ticketName = "일반 티켓",
                orderUuid = "order-uuid-1",
                enteredAt = null,
                status = IssuedTicketStatus.ENTRANCE_INCOMPLETE,
                createdAt = now,
            )

            val bytes = adminExcelService.generateIssuedTicketsExcelWithOptions(
                tickets = listOf(ticketResponse),
                options = listOf(option1, option2),
                issuedTickets = listOf(issuedTicket),
            )
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)
            val dataRow = sheet.getRow(1)

            // 기본 컬럼 확인
            assertEquals("T100001", dataRow.getCell(0).stringCellValue)
            assertEquals("테스트유저", dataRow.getCell(1).stringCellValue)
            assertEquals("일반 티켓", dataRow.getCell(2).stringCellValue)
            assertEquals("order-uuid-1", dataRow.getCell(3).stringCellValue)
            assertEquals("미입장", dataRow.getCell(4).stringCellValue)

            // 옵션 응답 컬럼 확인
            assertEquals("홍길동", dataRow.getCell(6).stringCellValue)
            assertEquals("010-1234-5678", dataRow.getCell(7).stringCellValue)
            workbook.close()
        }

        @Test
        @DisplayName("옵션 응답이 없는 경우 빈 문자열로 채워진다")
        fun missingOptionAnswersAreFilled() {
            val optionGroup = createOptionGroup(1L, "참석자 정보", OptionGroupType.SUBJECTIVE)
            val option1 = createOption(10L, "참석자 이름", optionGroup)
            val option2 = createOption(11L, "연락처", optionGroup)

            val now = LocalDateTime.of(2026, 3, 22, 10, 0)

            // 옵션 응답이 하나만 있는 경우
            val answer1 = IssuedTicketOptionAnswer(optionId = 10L, answer = "홍길동")
            val issuedTicket = createIssuedTicket(100L, listOf(answer1))

            val ticketResponse = AdminIssuedTicketResponse(
                id = 100L,
                issuedTicketNo = "T100001",
                userName = "테스트유저",
                ticketName = "일반 티켓",
                orderUuid = "order-uuid-1",
                enteredAt = null,
                status = IssuedTicketStatus.ENTRANCE_INCOMPLETE,
                createdAt = now,
            )

            val bytes = adminExcelService.generateIssuedTicketsExcelWithOptions(
                tickets = listOf(ticketResponse),
                options = listOf(option1, option2),
                issuedTickets = listOf(issuedTicket),
            )
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)
            val dataRow = sheet.getRow(1)

            assertEquals("홍길동", dataRow.getCell(6).stringCellValue)
            assertEquals("", dataRow.getCell(7).stringCellValue) // 응답 없음
            workbook.close()
        }
    }

    // --- Helper Methods ---

    private fun createOptionGroup(id: Long, name: String, type: OptionGroupType): OptionGroup {
        val optionGroup = OptionGroup(
            eventId = 1L,
            type = type,
            name = name,
            description = "설명",
            isEssential = true,
        )
        ReflectionTestUtils.setField(optionGroup, "id", id)
        return optionGroup
    }

    private fun createOption(id: Long, questionName: String, optionGroup: OptionGroup): Option {
        val og = OptionGroup(
            eventId = 1L,
            type = optionGroup.type,
            name = questionName,
            description = optionGroup.description,
            isEssential = optionGroup.isEssential,
        )
        ReflectionTestUtils.setField(og, "id", optionGroup.id)
        val option = Option(answer = "", additionalPrice = Money.ZERO, optionGroup = og)
        ReflectionTestUtils.setField(option, "id", id)
        return option
    }

    private fun createIssuedTicket(id: Long, optionAnswers: List<IssuedTicketOptionAnswer>): IssuedTicket {
        val ticket = IssuedTicket(
            eventId = 1L,
            initialOptionAnswers = optionAnswers,
        )
        ReflectionTestUtils.setField(ticket, "id", id)
        return ticket
    }
}
