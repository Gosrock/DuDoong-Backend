package band.gosrock.api.v2.operation.dto.response

import band.gosrock.api.v2.ticket.dto.V2TicketPayType
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayStatus
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/** D-1 대시보드 */
data class V2DashboardResponse(
    val orders: V2DashboardOrdersResponse,
    val tickets: V2DashboardTicketsResponse,
    @field:Schema(description = "판매금액 합계(원) = 승인·결제 완료 주문 결제금액 합")
    val salesAmount: Long,
    val entrance: V2EntranceStatsResponse,
    @field:Schema(description = "표시용 상태 (PREPARING / UPCOMING / ONGOING / PAST, #740)")
    val displayStatus: V2EventDisplayStatus,
    // getDDay() 는 Jackson 기본 규칙으로 "dday" 가 되므로 이름을 명시한다
    @get:JsonProperty("dDay")
    @field:Schema(description = "공연일까지 남은 일수 (D-n 의 n, 당일 0). UPCOMING 일 때만, 아니면 null")
    val dDay: Long?,
)

data class V2DashboardOrdersResponse(
    val pendingApprove: Long,
    val approved: Long,
    val refused: Long,
    @field:Schema(description = "환불 요청(미완료) 주문 수")
    val refundRequested: Long,
)

data class V2DashboardTicketsResponse(
    @field:Schema(description = "총 판매 매수 (재고 감소 기준)")
    val totalSoldCount: Long,
    @field:Schema(description = "총 판매 수량. 무제한 티켓이 하나라도 있으면 null (DEC-020)")
    val totalSupplyCount: Long?,
    val items: List<V2DashboardTicketItemResponse>,
)

data class V2DashboardTicketItemResponse(
    val ticketItemId: Long,
    val name: String?,
    val payType: V2TicketPayType?,
    val soldCount: Long,
    @field:Schema(description = "판매 수량. null 이면 무제한 (DEC-020)")
    val supplyCount: Long?,
    @field:Schema(description = "승인 대기 주문 수량 합 (#726, 판매 매수·재고와 별개)")
    val pendingApproveCount: Long,
)
