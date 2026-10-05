package band.gosrock.api.v2.operation.usecase

import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.OrderLineItem
import band.gosrock.domain.domains.order.domain.OrderOptionAnswer
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.test.util.ReflectionTestUtils

/** R-6 옵션 셀 규칙 (#730): API 로 만들 수 없는 값(빈 응답, 수량 없음)까지 단위로 고정한다 */
@DisplayName("v2 주문 엑셀 옵션 셀")
class V2ExcelOptionCellsTest {

    private val mapper = V2OperationMapper(
        mock(UserAdaptor::class.java), mock(EventAdaptor::class.java), mock(HostAdaptor::class.java),
        mock(OrderAdaptor::class.java), mock(OptionAdaptor::class.java),
    )

    /** 옵션 행 1 → 그룹 10, 옵션 행 2 → 그룹 20 */
    private val columns = V2ExcelOptionColumns(groupIds = listOf(10L, 20L), headers = listOf("a", "b"), groupOfOption = mapOf(1L to 10L, 2L to 20L))

    private var nextId = 1L

    private fun answer(optionId: Long, text: String?) = OrderOptionAnswer().also {
        ReflectionTestUtils.setField(it, "optionId", optionId)
        ReflectionTestUtils.setField(it, "answer", text)
    }

    private fun line(quantity: Long?, vararg answers: OrderOptionAnswer) =
        OrderLineItem.forTest(orderOptionAnswer = answers.toList(), quantity = quantity).also { ReflectionTestUtils.setField(it, "id", nextId++) }

    @Test
    fun `라인 1개는 응답 그대로 (수량 표기 없음)`() {
        assertEquals(mapOf(10L to "예", 20L to "메모"), mapper.excelOptionCells(listOf(line(3, answer(1, "예"), answer(2, "메모"))), columns))
    }

    @Test
    fun `여러 라인은 같은 응답 합산, 처음 나온 순서, 줄바꿈 연결, 수량 없으면 1`() {
        val lines = listOf(line(1, answer(1, "예")), line(null, answer(1, "아니요")), line(2, answer(1, "예")))
        assertEquals(mapOf(10L to "예 ×3\n아니요 ×1"), mapper.excelOptionCells(lines, columns))
    }

    @Test
    fun `빈 응답·null 은 빼고, 응답 안 줄바꿈은 공백, 컬럼에 없는 옵션은 무시, 응답이 없는 그룹은 맵에 없음`() {
        val lines = listOf(
            line(1, answer(1, "  "), answer(2, "첫줄\r\n둘째\n셋째"), answer(99, "모름")),
            line(1, answer(1, null), answer(2, "첫줄 둘째 셋째")),
        )
        assertEquals(mapOf(20L to "첫줄 둘째 셋째 ×2"), mapper.excelOptionCells(lines, columns))
    }
}
