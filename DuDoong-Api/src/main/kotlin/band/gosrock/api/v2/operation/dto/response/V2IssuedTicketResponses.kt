package band.gosrock.api.v2.operation.dto.response

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.ticket.dto.V2TicketPayType
import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceState
import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceStats
import band.gosrock.domain.domains.issuedTicket.service.v2.V2HostGiftState
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
    @field:Schema(description = "주문자(주문한 사용자) 이름 — 현재 회원 이름. 선물을 보내도 바뀌지 않는다 (#740 전에는 현재 소유자였음)")
    val buyerName: String?,
    @field:Schema(description = "현재 소유자 이름 — 선물이 수락되면 받은 사람. 선물이 없으면 주문자와 같다 (#740)")
    val ownerName: String?,
    @field:Schema(description = "선물 상태 NONE / PENDING(선물 대기 — 입장 불가) / ACCEPTED(선물 완료 — 소유자 = 받은 사람) (#740). " +
            "공연이 끝난 뒤에도 수락되지 않은 선물은 PENDING 그대로라 '선물 대기'로 보인다 (사용자 화면의 '선물 만료'는 조회 시 판정, DEC-026 #7)")
    val giftState: V2HostGiftState,
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
    @field:Schema(description = "주문자 연락처 (상세에서만, #740 전에는 현재 소유자 연락처였음)")
    val buyerPhone: String?,
    @field:Schema(description = "현재 소유자 연락처 (상세에서만, #740). 선물이 없으면 주문자 연락처와 같다")
    val ownerPhone: String?,
    val optionAnswers: List<V2OptionAnswerResponse>,
)
