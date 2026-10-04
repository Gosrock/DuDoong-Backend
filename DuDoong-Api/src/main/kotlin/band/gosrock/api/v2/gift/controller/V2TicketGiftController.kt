package band.gosrock.api.v2.gift.controller

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.gift.dto.V2GiftDirection
import band.gosrock.api.v2.gift.dto.V2MyTicketSort
import band.gosrock.api.v2.gift.dto.request.V2GiftMemoRequest
import band.gosrock.api.v2.gift.dto.response.V2GiftCreatedResponse
import band.gosrock.api.v2.gift.dto.response.V2GiftElement
import band.gosrock.api.v2.gift.dto.response.V2GiftLandingResponse
import band.gosrock.api.v2.gift.dto.response.V2GiftResultResponse
import band.gosrock.api.v2.gift.dto.response.V2MyTicketDetailResponse
import band.gosrock.api.v2.gift.dto.response.V2MyTicketsResponse
import band.gosrock.api.v2.gift.usecase.V2GiftUseCase
import band.gosrock.api.v2.gift.usecase.V2MyTicketUseCase
import band.gosrock.common.annotation.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 사용자 앱 티켓탭·선물 (#719, 11 문서 8장). G-3 랜딩만 비로그인 허용 (SecurityConfig.V2_PUBLIC_GET_PATHS, 로그인했으면 사용자를 읽는다) */
@SecurityRequirement(name = "access-token")
@Tag(name = "v2. 사용자 앱 - 티켓탭·선물")
@RestController
@RequestMapping("/api/v2")
@Validated
class V2TicketGiftController(
    private val myTicketUseCase: V2MyTicketUseCase,
    private val giftUseCase: V2GiftUseCase,
) {
    @Operation(
        summary = "[T-1] 내 티켓 (로그인). 주문 단위 묶음, 공연 임박순. 범위: 지금 소유한 티켓 + 내가 선물해 수락된 티켓(선물 완료, uuid 없음) + 승인 대기·거절 주문. " +
            "지난 공연·취소 티켓 포함, 페이지 없음",
    )
    @GetMapping("/me/tickets")
    fun getMyTickets(
        @CurrentUserId userId: Long,
        @RequestParam(defaultValue = "UPCOMING") sort: V2MyTicketSort,
    ): V2MyTicketsResponse = myTicketUseCase.tickets(userId)

    @Operation(summary = "[T-2] 티켓 상세 + 입장 QR (로그인, 지금 소유한 티켓만 — 그 밖 IssuedTicket_404_1). 선물 대기·취소 티켓은 qrValue null")
    @GetMapping("/me/tickets/{ticketUuid}")
    fun getMyTicket(@CurrentUserId userId: Long, @PathVariable ticketUuid: String): V2MyTicketDetailResponse =
        myTicketUseCase.ticket(userId, ticketUuid)

    @Operation(
        summary = "[G-1] 선물 링크 생성 (로그인, 주문자 = 소유자). 입장 전·공연 시작 전·원 주문 승인/확정·대기 선물 없음. " +
            "생성 후 티켓은 선물 대기 — 모든 입장 경로에서 막히고, 수락 전까지 만료 없음",
    )
    @PostMapping("/me/tickets/{ticketUuid}/gift")
    fun createGift(
        @CurrentUserId userId: Long,
        @PathVariable ticketUuid: String,
        @RequestBody(required = false) request: V2GiftMemoRequest?,
    ): V2GiftCreatedResponse = giftUseCase.create(userId, ticketUuid, request?.memo)

    @Operation(summary = "[G-6] 수락 후 반환 (로그인, 받은 사람). 입장 전·공연 시작 전. 보낸 사람에게 돌아가고 티켓 uuid(QR)가 바뀐다")
    @PostMapping("/me/tickets/{ticketUuid}/return")
    fun returnGift(@CurrentUserId userId: Long, @PathVariable ticketUuid: String): V2GiftResultResponse =
        giftUseCase.returnTicket(userId, ticketUuid)

    @Operation(summary = "[G-7] 보낸/받은 선물 내역 (로그인, 최신 순). 메모·링크는 보낸 사람에게만")
    @GetMapping("/me/gifts")
    fun getMyGifts(
        @CurrentUserId userId: Long,
        @RequestParam direction: V2GiftDirection,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) size: Int,
    ): V2PageResponse<V2GiftElement> = giftUseCase.history(userId, direction, page, size)

    @Operation(summary = "[G-2] 선물 취소(회수) (로그인, 보낸 사람). 대기 중이면 언제든(공연 시작·종료 후에도). 링크 무효, 티켓 복귀. 알림 없음")
    @DeleteMapping("/me/gifts/{giftId}")
    fun cancelGift(@CurrentUserId userId: Long, @PathVariable giftId: Long): V2GiftResultResponse =
        giftUseCase.cancel(userId, giftId)

    @Operation(summary = "[G-8] 메모 수정 (로그인, 보낸 사람, 대기 중일 때만). 빈 값이면 메모 삭제")
    @PatchMapping("/me/gifts/{giftId}")
    fun changeGiftMemo(@CurrentUserId userId: Long, @PathVariable giftId: Long, @RequestBody request: V2GiftMemoRequest): V2GiftResultResponse =
        giftUseCase.changeMemo(userId, giftId, request.memo)

    @Operation(
        summary = "[G-3] 선물 랜딩 (비로그인 가능). 보는 사람 기준 viewState: 선물 상태 → 만료(EXPIRED) → 본인 링크(OWN_LINK) → AVAILABLE. " +
            "대기 중이 아니면 상태만. 없는 토큰은 Gift_404_1",
    )
    @GetMapping("/gifts/{giftToken}")
    fun getGift(@CurrentUserId userId: Long, @PathVariable giftToken: String): V2GiftLandingResponse =
        giftUseCase.landing(userId, giftToken)

    @Operation(summary = "[G-4] 선물 받기 (로그인). 대기 중·공연 종료 전·본인 링크 아님. 소유자 변경 + 티켓 uuid(QR) 새로 발급. 같은 링크 동시 수락은 1명만")
    @PostMapping("/gifts/{giftToken}/accept")
    fun acceptGift(@CurrentUserId userId: Long, @PathVariable giftToken: String): V2GiftResultResponse =
        giftUseCase.accept(userId, giftToken)

    @Operation(summary = "[G-5] 선물 거절 (로그인). 수락과 같은 조건. 티켓은 보낸 사람에게 그대로")
    @PostMapping("/gifts/{giftToken}/reject")
    fun rejectGift(@CurrentUserId userId: Long, @PathVariable giftToken: String): V2GiftResultResponse =
        giftUseCase.reject(userId, giftToken)

    companion object {
        const val MAX_PAGE_SIZE = 50L
    }
}
