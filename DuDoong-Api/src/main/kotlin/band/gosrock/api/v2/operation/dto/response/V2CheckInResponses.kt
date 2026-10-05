package band.gosrock.api.v2.operation.dto.response

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.issuedTicket.service.v2.V2CheckInResult
import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceState
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** Q-2 / Q-5 체크인 결과 (항상 200) */
data class V2CheckInResponse(
    @field:Schema(
        description = "ENTERED(입장 완료) / ALREADY_ENTERED(이미 입장) / OTHER_EVENT(이 공연 티켓 아님 — 없는 티켓 포함, 셀프는 이 공연 본인 티켓 없음) / " +
            "CANCELED(취소된 티켓) / SELECT_TICKET(셀프 전용: 입장 전 티켓이 여러 장 → candidates 중 하나로 다시 요청) / " +
            "GIFT_PENDING(선물 대기 중인 티켓 — 입장 안 함, 보낸 사람이 선물을 취소해야 입장 가능. 셀프 후보에서도 제외, #719)",
    )
    val result: V2CheckInResult,
    @field:Schema(description = "판정한 티켓 요약. OTHER_EVENT·SELECT_TICKET 이면 null")
    val ticket: V2CheckInTicketResponse?,
    @field:Schema(description = "SELECT_TICKET 일 때 입장 전 본인 티켓 (그 외 빈 목록)")
    val candidates: List<V2CheckInTicketResponse>,
)

data class V2CheckInTicketResponse(
    val ticketUuid: String?,
    val issuedTicketNo: String?,
    val ticketName: String?,
    val buyerName: String?,
    val entrance: V2EntranceState,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val enteredAt: LocalDateTime?,
)

/** Q-4 셀프 체크인 QR */
data class V2CheckInQrResponse(
    @field:Schema(description = "공연별 고정 토큰 (base64url 43자). 노출되면 현장 밖 체크인이 가능하므로 호스트 화면에만 표시 (DEC-011)")
    val token: String,
    @field:Schema(description = "QR 에 넣을 경로. 프론트가 자기 origin 을 붙여 URL 로 만든다 (예: https://dudoong.com + qrPath)", example = "/check-in?token=abc")
    val qrPath: String,
)
