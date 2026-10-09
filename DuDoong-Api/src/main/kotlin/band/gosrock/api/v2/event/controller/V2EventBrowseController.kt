package band.gosrock.api.v2.event.controller

import band.gosrock.api.v2.common.V2Paging
import band.gosrock.api.v2.common.swagger.V2ApiArea
import band.gosrock.api.v2.common.swagger.V2ApiTags
import band.gosrock.api.v2.common.swagger.V2Area
import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.event.dto.request.V2EventSort
import band.gosrock.api.v2.event.dto.response.V2EventDetailResponse
import band.gosrock.api.v2.event.dto.response.V2EventListItemResponse
import band.gosrock.api.v2.event.dto.response.V2HomeResponse
import band.gosrock.api.v2.event.dto.response.V2PublicTicketItemResponse
import band.gosrock.api.v2.event.usecase.V2ReadEventDetailUseCase
import band.gosrock.api.v2.event.usecase.V2ReadHomeUseCase
import band.gosrock.api.v2.event.usecase.V2ReadOnSaleTicketItemsUseCase
import band.gosrock.api.v2.event.usecase.V2SearchEventsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 사용자 앱 공연 탐색 (#716). 모두 비로그인 허용 (SecurityConfig.V2_PUBLIC_GET_PATHS). P-4 섹션은 [V2EventController] E-5 */
@V2Area(V2ApiArea.USER)
@Tag(name = V2ApiTags.BROWSE, description = V2ApiTags.BROWSE_DESCRIPTION)
@RestController
@RequestMapping("/api/v2")
@Validated
class V2EventBrowseController(
    private val readHomeUseCase: V2ReadHomeUseCase,
    private val searchEventsUseCase: V2SearchEventsUseCase,
    private val readEventDetailUseCase: V2ReadEventDetailUseCase,
    private val readOnSaleTicketItemsUseCase: V2ReadOnSaleTicketItemsUseCase,
) {
    @Operation(summary = "[P-1] 홈: 종료 전 등록(OPEN) 공연(진행 중 + 시작 전) 시작 임박순 — 진행 중이 앞, 최대 10개 (비로그인 허용)")
    @GetMapping("/home")
    fun getHome(): V2HomeResponse = readHomeUseCase.execute()

    @Operation(
        summary = "[P-2] 공연 리스트 (비로그인 허용)",
        description = "keyword = 공연명 OR 호스트명 부분일치. tagIds = 같은 분류 OR / 분류끼리 AND (없는 태그 id 는 400). " +
            "includePast=false 면 종료 전 등록(OPEN) 공연(진행 중 + 시작 전)만, true 면 종료된 OPEN·정산중·지난공연 포함 (종료 = 시작 + 러닝타임). " +
            "sort=UPCOMING: 종료 전 공연 시작 임박순(진행 중이 앞) → 지난 공연(종료된 OPEN·정산중·지난공연) 최근 시작 순",
    )
    @GetMapping("/events")
    fun searchEvents(
        @Parameter(description = "공연명 OR 호스트명 (최대 ${V2Paging.KEYWORD_MAX_LENGTH} 자)")
        @RequestParam(required = false) @Size(max = V2Paging.KEYWORD_MAX_LENGTH) keyword: String?,
        @Parameter(description = "태그 id (쉼표 구분, 예: 1,2, 최대 $MAX_TAG_IDS 개)")
        @RequestParam(required = false) @Size(max = MAX_TAG_IDS) tagIds: List<Long>?,
        @RequestParam(defaultValue = "false") includePast: Boolean,
        @RequestParam(defaultValue = "UPCOMING") sort: V2EventSort,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = V2Paging.CARD_DEFAULT_SIZE) @Min(1) @Max(V2Paging.MAX_SIZE) size: Int,
    ): V2PageResponse<V2EventListItemResponse> = searchEventsUseCase.execute(keyword, tagIds.orEmpty(), includePast, page, size)

    @Operation(summary = "[P-3] 공개 공연 상세 (비로그인 허용). 준비중·삭제 공연은 404")
    @GetMapping("/events/{eventId}")
    fun getEventDetail(@PathVariable eventId: Long): V2EventDetailResponse = readEventDetailUseCase.execute(eventId)

    @Operation(summary = "[P-5] 판매 중인 티켓 (비로그인 허용). 판매 중단·판매 기간 밖·삭제 티켓 제외, 계좌 미노출. 준비중·삭제 공연은 404")
    @GetMapping("/events/{eventId}/ticket-items")
    fun getOnSaleTicketItems(@PathVariable eventId: Long): List<V2PublicTicketItemResponse> = readOnSaleTicketItemsUseCase.execute(eventId)

    companion object {
        /** 필터 태그 id 상한 (태그는 운영자 관리, 초기 22개). 넘으면 요청 검증 400 (없는 태그 id 의 Event_400_23 과 구분) */
        private const val MAX_TAG_IDS = 50
    }
}
