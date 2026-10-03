package band.gosrock.api.v2.operation.dto.response

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.ticket.dto.V2TicketPayType
import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceState
import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceStats
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** I-1 발급 티켓 목록 + 건수 */
data class V2IssuedTicketListResponse(
    @field:Schema(description = "입장 상태별 건수 (검색어 반영, 입장 필터 무시)")
    val counts: V2EntranceStatsResponse,
    val tickets: V2PageResponse<V2IssuedTicketElement>,
)

/** 유효(취소 제외) 티켓 기준 입장 통계. Q-1 / 대시보드 / I-1 건수 */
data class V2EntranceStatsResponse(
    @field:Schema(description = "전체 발급(유효) 티켓 수")
    val issuedCount: Long,
    val enteredCount: Long,
    val notEnteredCount: Long,
    @field:Schema(description = "입장률(%) 소수 첫째 자리", example = "42.9")
    val entranceRate: Double,
) {
    companion object {
        fun of(stats: V2EntranceStats) = V2EntranceStatsResponse(
            issuedCount = stats.issuedCount,
            enteredCount = stats.enteredCount,
            notEnteredCount = stats.notEnteredCount,
            entranceRate = stats.entranceRate,
        )
    }
}

data class V2IssuedTicketElement(
    val ticketUuid: String?,
    @field:Schema(description = "티켓 번호 (T1000xxxx)")
    val issuedTicketNo: String?,
    val ticketItemId: Long?,
    @field:Schema(description = "티켓 종류 DUDOONG / FREE / PRICE")
    val payType: V2TicketPayType?,
    val ticketName: String?,
    val buyerName: String?,
    val orderUuid: String?,
    val orderNo: String?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "발급 일시")
    val issuedAt: LocalDateTime?,
    @field:Schema(description = "BEFORE / DONE (상세에서는 CANCELED 도 가능)")
    val entrance: V2EntranceState,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "체크인 시각")
    val enteredAt: LocalDateTime?,
)

/** I-2 발급 티켓 상세 */
data class V2IssuedTicketDetailResponse(
    val ticket: V2IssuedTicketElement,
    @field:Schema(description = "주문자 연락처 (상세에서만)")
    val buyerPhone: String?,
    val optionAnswers: List<V2OptionAnswerResponse>,
)
