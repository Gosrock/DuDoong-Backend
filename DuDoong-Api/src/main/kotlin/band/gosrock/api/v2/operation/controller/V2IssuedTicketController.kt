package band.gosrock.api.v2.operation.controller

import band.gosrock.api.v2.common.V2Paging
import band.gosrock.api.v2.common.swagger.V2AlsoIn
import band.gosrock.api.v2.common.swagger.V2ApiArea
import band.gosrock.api.v2.common.swagger.V2ApiTags
import band.gosrock.api.v2.operation.dto.V2EntranceFilter
import band.gosrock.api.v2.operation.dto.request.V2CheckInRequest
import band.gosrock.api.v2.operation.dto.request.V2SelfCheckInRequest
import band.gosrock.api.v2.operation.dto.response.V2CheckInQrResponse
import band.gosrock.api.v2.operation.dto.response.V2CheckInResponse
import band.gosrock.api.v2.operation.dto.response.V2EntranceStatsResponse
import band.gosrock.api.v2.operation.dto.response.V2IssuedTicketDetailResponse
import band.gosrock.api.v2.operation.dto.response.V2IssuedTicketListResponse
import band.gosrock.api.v2.operation.usecase.V2CheckInUseCase
import band.gosrock.api.v2.operation.usecase.V2ReadIssuedTicketsUseCase
import band.gosrock.common.annotation.CurrentUserId
import band.gosrock.domain.domains.order.repository.condition.AdminTableSearchType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = V2ApiTags.OPERATION_TICKET, description = V2ApiTags.OPERATION_TICKET_DESCRIPTION)
@RestController
@RequestMapping("/api/v2")
@Validated
class V2IssuedTicketController(
    private val readIssuedTicketsUseCase: V2ReadIssuedTicketsUseCase,
    private val checkInUseCase: V2CheckInUseCase,
) {
    @Operation(summary = "[I-1] 발급 티켓 목록 (일반 멤버 이상). 취소 티켓 제외, 최신 순, 입장 상태별 건수 포함")
    @GetMapping("/events/{eventId}/issued-tickets")
    fun getIssuedTickets(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @RequestParam(defaultValue = "ALL") entrance: V2EntranceFilter,
        @Parameter(description = SEARCH_TYPE_DESCRIPTION)
        @RequestParam(required = false) searchType: AdminTableSearchType?,
        @Parameter(description = "검색어 (searchType 기준, 현재 소유자의 이름 또는 연락처 부분일치)")
        @RequestParam(required = false) @Size(max = V2Paging.KEYWORD_MAX_LENGTH) keyword: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = V2Paging.DEFAULT_SIZE) @Min(1) @Max(V2Paging.TABLE_MAX_SIZE) size: Int,
    ): V2IssuedTicketListResponse = readIssuedTicketsUseCase.execute(userId, eventId, entrance, searchType, keyword, page, size)

    @Operation(
        summary = "[I-3] 발급 티켓 엑셀 다운로드 (일반 멤버 이상). I-1 과 같은 필터 + 옵션 응답 컬럼 (xlsx)",
        responses = [ApiResponse(responseCode = "200", description = "xlsx 파일 (응답 래퍼 없음)", content = [Content(mediaType = V2Excel.XLSX_MEDIA_TYPE, schema = Schema(type = "string", format = "binary"))])],
    )
    @GetMapping("/events/{eventId}/issued-tickets/export")
    fun exportIssuedTickets(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @RequestParam(defaultValue = "ALL") entrance: V2EntranceFilter,
        @Parameter(description = SEARCH_TYPE_DESCRIPTION)
        @RequestParam(required = false) searchType: AdminTableSearchType?,
        @Parameter(description = "검색어 (searchType 기준, 현재 소유자의 이름 또는 연락처 부분일치)")
        @RequestParam(required = false) @Size(max = V2Paging.KEYWORD_MAX_LENGTH) keyword: String?,
    ): ResponseEntity<ByteArray> =
        V2Excel.attachment("issued-tickets-$eventId.xlsx", readIssuedTicketsUseCase.export(userId, eventId, entrance, searchType, keyword))

    @Operation(summary = "[I-2] 발급 티켓 상세 (일반 멤버 이상). 옵션 응답 포함, 다른 공연 티켓은 404")
    @GetMapping("/events/{eventId}/issued-tickets/{ticketUuid}")
    fun getIssuedTicket(@CurrentUserId userId: Long, @PathVariable eventId: Long, @PathVariable ticketUuid: String): V2IssuedTicketDetailResponse =
        readIssuedTicketsUseCase.detail(userId, eventId, ticketUuid)

    @Operation(summary = "[Q-1] 체크인 통계 (일반 멤버 이상). 전체 발급(유효)·입장 완료·미입장·입장률")
    @GetMapping("/events/{eventId}/check-ins/stats")
    fun getCheckInStats(@CurrentUserId userId: Long, @PathVariable eventId: Long): V2EntranceStatsResponse =
        checkInUseCase.stats(userId, eventId)

    @Operation(summary = "[Q-2] 호스트 QR 스캔 입장 (일반 멤버 이상, DEC-009). 항상 200 + result 4종 (DEC-010). 체크인 취소 없음")
    @PostMapping("/events/{eventId}/check-ins")
    fun checkIn(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @RequestBody @Valid request: V2CheckInRequest,
    ): V2CheckInResponse = checkInUseCase.checkIn(userId, eventId, request.ticketUuid!!)

    @Operation(summary = "[Q-4] 관객 셀프 체크인 QR (일반 멤버 이상, DEC-011). 공연별 고정 토큰, 없으면 이때 생성")
    @GetMapping("/events/{eventId}/check-in-qr")
    fun getCheckInQr(@CurrentUserId userId: Long, @PathVariable eventId: Long): V2CheckInQrResponse =
        checkInUseCase.qr(userId, eventId)

    @V2AlsoIn(V2ApiArea.USER)
    @Operation(summary = "[Q-5] 관객 셀프 체크인 (로그인 유저). 토큰 공연(OPEN)의 본인 티켓 입장. 여러 장이면 SELECT_TICKET + candidates → ticketUuid 로 재요청")
    @PostMapping("/check-ins/self")
    fun selfCheckIn(@CurrentUserId userId: Long, @RequestBody @Valid request: V2SelfCheckInRequest): V2CheckInResponse =
        checkInUseCase.selfCheckIn(userId, request.token!!, request.ticketUuid)

    companion object {
        /** 발급 티켓 검색 기준 (#740): 현재 소유자 기준 — 선물이 수락된 티켓은 받은 사람으로 찾는다 */
        private const val SEARCH_TYPE_DESCRIPTION =
            "검색 기준 NAME(기본) / PHONE — **현재 소유자**(선물 수락 시 받은 사람)의 이름·연락처. 주문자 이름으로는 찾지 않는다"
    }
}
