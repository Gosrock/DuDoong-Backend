package band.gosrock.api.v2.ticket.controller

import band.gosrock.api.v2.ticket.dto.request.V2ReplaceTicketOptionsRequest
import band.gosrock.api.v2.ticket.dto.request.V2TicketItemRequest
import band.gosrock.api.v2.ticket.dto.response.V2TicketItemManageResponse
import band.gosrock.api.v2.ticket.usecase.V2ChangeTicketItemSellableUseCase
import band.gosrock.api.v2.ticket.usecase.V2CreateTicketItemUseCase
import band.gosrock.api.v2.ticket.usecase.V2DeleteTicketItemUseCase
import band.gosrock.api.v2.ticket.usecase.V2ReadTicketItemsUseCase
import band.gosrock.api.v2.ticket.usecase.V2ReplaceTicketItemOptionsUseCase
import band.gosrock.api.v2.ticket.usecase.V2UpdateTicketItemUseCase
import band.gosrock.common.annotation.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = "v2. 티켓")
@RestController
@RequestMapping("/api/v2/events/{eventId}/ticket-items")
class V2TicketItemController(
    private val readTicketItemsUseCase: V2ReadTicketItemsUseCase,
    private val createTicketItemUseCase: V2CreateTicketItemUseCase,
    private val updateTicketItemUseCase: V2UpdateTicketItemUseCase,
    private val deleteTicketItemUseCase: V2DeleteTicketItemUseCase,
    private val changeTicketItemSellableUseCase: V2ChangeTicketItemSellableUseCase,
    private val replaceTicketItemOptionsUseCase: V2ReplaceTicketItemOptionsUseCase,
) {
    @Operation(summary = "[T-1] 관리용 티켓 목록 (일반 멤버 이상). 재고 항상 공개, saleState·적용 옵션 포함")
    @GetMapping("/manage")
    fun getTicketItems(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
    ): List<V2TicketItemManageResponse> = readTicketItemsUseCase.execute(userId, eventId)

    @Operation(summary = "[T-2] 티켓 생성 (매니저 이상). DUDOONG / FREE 만, 티켓 없음 공연 불가")
    @PostMapping
    fun createTicketItem(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @RequestBody @Valid request: V2TicketItemRequest,
    ): V2TicketItemManageResponse = createTicketItemUseCase.execute(userId, eventId, request)

    @Operation(summary = "[T-3] 티켓 수정 (매니저 이상). 폼 전체 전송. 판매된 티켓은 설명·판매기간·재고공개·매수제한·수량 증가만")
    @PatchMapping("/{ticketItemId}")
    fun updateTicketItem(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @PathVariable ticketItemId: Long,
        @RequestBody @Valid request: V2TicketItemRequest,
    ): V2TicketItemManageResponse = updateTicketItemUseCase.execute(userId, eventId, ticketItemId, request)

    @Operation(summary = "[T-4] 티켓 삭제 (매니저 이상). 재고 감소·승인 대기 주문이 없을 때만. 남은 티켓 목록 반환")
    @DeleteMapping("/{ticketItemId}")
    fun deleteTicketItem(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @PathVariable ticketItemId: Long,
    ): List<V2TicketItemManageResponse> = deleteTicketItemUseCase.execute(userId, eventId, ticketItemId)

    @Operation(summary = "[T-5] 판매 중단 (매니저 이상). 이미 중단이면 그대로 200")
    @PostMapping("/{ticketItemId}/suspend")
    fun suspendTicketItem(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @PathVariable ticketItemId: Long,
    ): V2TicketItemManageResponse = changeTicketItemSellableUseCase.execute(userId, eventId, ticketItemId, sellable = false)

    @Operation(summary = "[T-6] 판매 재개 (매니저 이상). 이미 판매 중이면 그대로 200")
    @PostMapping("/{ticketItemId}/resume")
    fun resumeTicketItem(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @PathVariable ticketItemId: Long,
    ): V2TicketItemManageResponse = changeTicketItemSellableUseCase.execute(userId, eventId, ticketItemId, sellable = true)

    @Operation(summary = "[O-5] 티켓에 붙은 옵션 전체 지정 (매니저 이상). 판매된 티켓은 변경 불가")
    @PutMapping("/{ticketItemId}/options")
    fun replaceTicketItemOptions(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @PathVariable ticketItemId: Long,
        @RequestBody @Valid request: V2ReplaceTicketOptionsRequest,
    ): V2TicketItemManageResponse = replaceTicketItemOptionsUseCase.execute(userId, eventId, ticketItemId, request)
}
