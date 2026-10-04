package band.gosrock.api.v2.event.dto.response

import band.gosrock.api.v2.tag.dto.V2TagResponse
import band.gosrock.api.v2.ticket.dto.V2TicketOptionType
import band.gosrock.api.v2.ticket.dto.V2TicketPayType
import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.common.vo.HostContactVo
import band.gosrock.domain.domains.event.service.v2.V2EventBrowseDisplayStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** P-1 홈 캐러셀 공연 */
data class V2HomeEventResponse(
    val eventId: Long,
    val name: String?,
    val posterImageUrl: String?,
    @field:DateFormat
    val startAt: LocalDateTime?,
    val placeName: String?,
    val hostName: String?,
)

/** P-1 홈 */
data class V2HomeResponse(
    @field:Schema(description = "등록(OPEN)·시작 전 공연, 시작 임박순 최대 10개")
    val events: List<V2HomeEventResponse>,
)

/** P-2 공연 리스트 항목 */
data class V2EventListItemResponse(
    val eventId: Long,
    val name: String?,
    val posterImageUrl: String?,
    @field:DateFormat
    val startAt: LocalDateTime?,
    @field:DateFormat
    val endAt: LocalDateTime?,
    val placeName: String?,
    val hostName: String?,
    @field:Schema(description = "태그 (분류 순 → 분류 안 순서)")
    val tags: List<V2TagResponse>,
    @field:Schema(description = "UPCOMING(등록·시작 전) / PAST(시작한 공연·정산중·지난공연)")
    val displayStatus: V2EventBrowseDisplayStatus,
)

/** P-3 공개 공연 상세. 호스트 멤버·슬랙 url·체크인 토큰 등 내부 정보는 노출하지 않는다 */
data class V2EventDetailResponse(
    val eventId: Long,
    val name: String?,
    val posterImageUrl: String?,
    @field:DateFormat
    val startAt: LocalDateTime?,
    @field:DateFormat
    val endAt: LocalDateTime?,
    @field:Schema(description = "진행 시간(분)")
    val runTime: Long?,
    @field:Schema(description = "공연 장소. 미입력이면 null")
    val place: V2EventPlaceResponse?,
    @field:Schema(description = "티켓 판매 공연인지. false 면 티켓 없이 정보만 공개하는 공연")
    val hasTicket: Boolean,
    @field:Schema(description = "태그 (분류 순 → 분류 안 순서)")
    val tags: List<V2TagResponse>,
    val host: V2EventHostSummaryResponse,
    @field:Schema(description = "문의처. 공연 문의처가 없으면 호스트 연락처(v2 연락처, 없으면 v1 전화/이메일)")
    val contacts: List<HostContactVo>,
    @field:Schema(description = "UPCOMING(등록·시작 전) / PAST(시작한 공연·정산중·지난공연)")
    val displayStatus: V2EventBrowseDisplayStatus,
)

data class V2EventHostSummaryResponse(
    val hostId: Long,
    val name: String?,
    val profileImageUrl: String?,
)

/** P-5 판매 중인 티켓 (사용자용). 입금 계좌는 주문 단계에서 제공하므로 여기서는 내려주지 않는다 */
data class V2PublicTicketItemResponse(
    val ticketItemId: Long,
    val name: String?,
    val description: String?,
    @field:Schema(description = "가격(원)")
    val price: Long,
    @field:Schema(description = "DUDOONG / FREE / PRICE(기존 PG 티켓)")
    val payType: V2TicketPayType?,
    @field:Schema(description = "true = 호스트 승인 후 확정, false = 구매 후 자동 확정")
    val approvalRequired: Boolean,
    @field:Schema(description = "잔여 수량. 재고 공개 + 수량 지정 티켓만, 아니면 null")
    val remaining: Long?,
    val isSoldOut: Boolean,
    @field:Schema(description = "지금 살 수 있는지: 공연 등록(OPEN) + 공연 시작 전 + 판매 중 + 매진 아님")
    val isPurchasable: Boolean,
    @field:Schema(description = "1인 구매 매수 제한. null 이면 제한 없음")
    val purchaseLimit: Long?,
    @field:Schema(description = "붙은 옵션 (id 순)")
    val options: List<V2PublicTicketOptionResponse>,
)

data class V2PublicTicketOptionResponse(
    val optionId: Long,
    val name: String?,
    val description: String?,
    val type: V2TicketOptionType?,
    @field:Schema(description = "'네' 선택 시 추가 금액. YES_NO 가 아니면 null")
    val yesAdditionalPrice: Long?,
)
