package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminTicketItemResponse
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import java.io.ByteArrayInputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils

/** v2 무제한 수량·매수 제한 없음 저장값(1,000,000)을 어드민이 숫자로 노출하지 않는지 (#707) */
@DisplayName("어드민 티켓 무제한 표시")
class AdminTicketItemUnlimitedTest {

    private fun item(supplyCount: Long, purchaseLimit: Long, sold: Long = 0): TicketItem =
        TicketItem(
            payType = TicketPayType.FREE_TICKET,
            name = "무료",
            price = Money.ZERO,
            quantity = supplyCount - sold,
            supplyCount = supplyCount,
            purchaseLimit = purchaseLimit,
            type = TicketType.FIRST_COME_FIRST_SERVED,
            isSellable = true,
        ).also { ReflectionTestUtils.setField(it, "id", 1L) }

    @Test
    fun `응답은 숫자 필드를 유지하고 무제한·제한 없음 boolean 을 준다`() {
        val unlimited = AdminTicketItemResponse.from(item(TicketItem.UNLIMITED_SUPPLY_COUNT, TicketItem.NO_PURCHASE_LIMIT))
        assertTrue(unlimited.isUnlimitedSupply)
        assertTrue(unlimited.hasNoPurchaseLimit)
        assertEquals(TicketItem.UNLIMITED_SUPPLY_COUNT, unlimited.supplyCount)

        val limited = AdminTicketItemResponse.from(item(100, 4))
        assertFalse(limited.isUnlimitedSupply)
        assertFalse(limited.hasNoPurchaseLimit)
    }

    @Test
    fun `엑셀은 저장값 대신 무제한·제한 없음 문구`() {
        val bytes = AdminExcelService().generateTicketItemsExcel(
            listOf(
                AdminTicketItemResponse.from(item(TicketItem.UNLIMITED_SUPPLY_COUNT, TicketItem.NO_PURCHASE_LIMIT, sold = 3)),
                AdminTicketItemResponse.from(item(100, 4, sold = 3)),
            ),
        )
        XSSFWorkbook(ByteArrayInputStream(bytes)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            val unlimited = sheet.getRow(1)
            assertEquals("무제한", unlimited.getCell(3).stringCellValue)
            assertEquals("무제한", unlimited.getCell(4).stringCellValue)
            assertEquals("제한 없음", unlimited.getCell(5).stringCellValue)
            val limited = sheet.getRow(2)
            assertEquals(97.0, limited.getCell(3).numericCellValue)
            assertEquals(100.0, limited.getCell(4).numericCellValue)
            assertEquals(4.0, limited.getCell(5).numericCellValue)
        }
    }

    @Test
    fun `무제한 티켓의 어드민 재고 조정은 저장값을 유지한다`() {
        val unlimited = item(TicketItem.UNLIMITED_SUPPLY_COUNT, TicketItem.NO_PURCHASE_LIMIT, sold = 2)
        unlimited.adminAdjustStock(-5)
        assertEquals(TicketItem.UNLIMITED_SUPPLY_COUNT, unlimited.supplyCount)
        assertEquals(TicketItem.UNLIMITED_SUPPLY_COUNT - 2, unlimited.quantity)

        val limited = item(10, 1)
        limited.adminAdjustStock(5)
        assertEquals(15L, limited.supplyCount)
    }
}
