package band.gosrock.api.v2.operation.dto.request

import band.gosrock.domain.domains.issuedTicket.service.v2.V2CheckInDomainService
import band.gosrock.domain.domains.order.domain.OrderRefuseReasonType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/** R-4 거절 */
data class V2RefuseOrderRequest(
    @field:Schema(description = "DEPOSIT_UNCONFIRMED(입금 미확인) / AMOUNT_MISMATCH(결제 금액 오류) / SOLD_OUT(티켓 매진) / ETC(기타)", example = "ETC")
    @field:NotNull
    val reasonType: OrderRefuseReasonType?,

    @field:Schema(description = "기타(ETC) 사유. ETC 면 필수 1~20자(앞뒤 공백 제외, 아니면 Order_400_18), 그 외 종류는 무시", example = "중복 주문")
    @field:Size(max = RAW_TEXT_MAX)
    val reasonText: String? = null,
)

/** R-5 취소 */
data class V2CancelOrderRequest(
    @field:Schema(description = "취소 사유 (선택, 최대 100자)", example = "공연 일정 변경")
    @field:Size(max = 100)
    val reason: String? = null,
)

/** Q-2 호스트 스캔 */
data class V2CheckInRequest(
    @field:Schema(description = "관객 티켓 QR 의 발급 티켓 uuid")
    @field:NotBlank
    @field:Size(max = UUID_MAX)
    val ticketUuid: String?,
)

/** Q-5 관객 셀프 체크인 */
data class V2SelfCheckInRequest(
    @field:Schema(description = "공연 체크인 QR 토큰 (Q-4)")
    @field:NotBlank
    @field:Size(max = V2CheckInDomainService.TOKEN_MAX_LENGTH)
    val token: String?,

    @field:Schema(description = "입장할 본인 티켓 uuid. 입장 전 티켓이 여러 장이면 SELECT_TICKET 응답의 candidates 중 하나를 골라 다시 보낸다")
    @field:Size(max = UUID_MAX)
    val ticketUuid: String? = null,
)

/** DTO 1차 상한 (본 검증은 도메인) */
private const val RAW_TEXT_MAX = 200
private const val UUID_MAX = 64
