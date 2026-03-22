package band.gosrock.api.ticketItem.controller

import band.gosrock.api.ticketItem.dto.request.ApplyTicketOptionRequest
import band.gosrock.api.ticketItem.dto.request.CreateTicketItemRequest
import band.gosrock.api.ticketItem.dto.request.UnapplyTicketOptionRequest
import band.gosrock.api.ticketItem.dto.response.GetAppliedOptionGroupsResponse
import band.gosrock.api.ticketItem.dto.response.GetEventTicketItemsResponse
import band.gosrock.api.ticketItem.dto.response.GetTicketItemOptionsResponse
import band.gosrock.api.ticketItem.dto.response.TicketItemResponse
import band.gosrock.api.ticketItem.service.ApplyTicketOptionUseCase
import band.gosrock.api.ticketItem.service.DeleteTicketItemUseCase
import band.gosrock.api.ticketItem.service.GetAppliedOptionGroupsUseCase
import band.gosrock.api.ticketItem.service.GetEventTicketItemsUseCase
import band.gosrock.api.ticketItem.service.GetTicketOptionsUseCase
import band.gosrock.api.ticketItem.service.UnapplyTicketOptionUseCase
import band.gosrock.api.ticketItem.service.CreateTicketItemUseCase
import band.gosrock.common.annotation.CurrentUserId
import band.gosrock.common.annotation.DisableSwaggerSecurity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = "7-1. [티켓상품]")
@RestController
@RequestMapping("/api/v1/events/{eventId}/ticketItems")
class TicketItemController(
    val createTicketItemUseCase: CreateTicketItemUseCase,
    val applyTicketOptionUseCase: ApplyTicketOptionUseCase,
    val getTicketOptionsUseCase: GetTicketOptionsUseCase,
    val getEventTicketItemsUseCase: GetEventTicketItemsUseCase,
    val deleteTicketItemUseCase: DeleteTicketItemUseCase,
    val getAppliedOptionGroupsUseCase: GetAppliedOptionGroupsUseCase,
    val unapplyTicketOptionUseCase: UnapplyTicketOptionUseCase,
) {

    @Operation(
        summary = "특정 이벤트에 속하는 티켓 상품을 생성합니다.",
        description = "두둥티켓은 승인형식만, 유료티켓은 선착순형식만 가능합니다.",
    )
    @PostMapping
    fun createTicketItem(
        @CurrentUserId userId: Long,
        @RequestBody @Valid createTicketItemRequest: CreateTicketItemRequest,
        @PathVariable eventId: Long,
    ): TicketItemResponse = createTicketItemUseCase.execute(userId, createTicketItemRequest, eventId)

    @Operation(summary = "옵션을 티켓상품에 적용합니다.")
    @PatchMapping("/{ticketItemId}/option")
    fun applyTicketOption(
        @CurrentUserId userId: Long,
        @RequestBody @Valid applyTicketOptionRequest: ApplyTicketOptionRequest,
        @PathVariable eventId: Long,
        @PathVariable ticketItemId: Long,
    ): GetTicketItemOptionsResponse = applyTicketOptionUseCase.execute(userId, applyTicketOptionRequest, eventId, ticketItemId)

    @Operation(summary = "옵션을 티켓상품에 적용 취소합니다.")
    @PatchMapping("/{ticketItemId}/option/cancel")
    fun unapplyTicketOption(
        @CurrentUserId userId: Long,
        @RequestBody @Valid unapplyTicketOptionRequest: UnapplyTicketOptionRequest,
        @PathVariable eventId: Long,
        @PathVariable ticketItemId: Long,
    ): GetTicketItemOptionsResponse = unapplyTicketOptionUseCase.execute(userId, unapplyTicketOptionRequest, eventId, ticketItemId)

    @Operation(summary = "해당 이벤트의 티켓상품을 모두 조회합니다.")
    @DisableSwaggerSecurity
    @GetMapping
    fun getEventTicketItems(@PathVariable eventId: Long): GetEventTicketItemsResponse =
        getEventTicketItemsUseCase.execute(eventId)

    @Operation(summary = "해당 이벤트의 티켓상품을 모두 조회합니다. (어드민용)", description = "재고 정보가 무조건 공개됩니다.")
    @GetMapping("/admin")
    fun getEventTicketItemsForAdmin(@CurrentUserId userId: Long, @PathVariable eventId: Long): GetEventTicketItemsResponse =
        getEventTicketItemsUseCase.executeForAdmin(userId, eventId)

    @Operation(summary = "해당 티켓상품의 옵션을 모두 조회합니다.")
    @GetMapping("/{ticketItemId}/options")
    fun getTicketItemOptions(
        @PathVariable eventId: Long,
        @PathVariable ticketItemId: Long,
    ): GetTicketItemOptionsResponse = getTicketOptionsUseCase.execute(eventId, ticketItemId)

    @Operation(summary = "해당 이벤트의 티켓상품 옵션 적용 현황을 모두 조회합니다.")
    @GetMapping("/appliedOptionGroups")
    fun getAppliedOptionGroups(@PathVariable eventId: Long): GetAppliedOptionGroupsResponse =
        getAppliedOptionGroupsUseCase.execute(eventId)

    @Operation(summary = "해당 티켓상품을 삭제합니다.")
    @PatchMapping("/{ticketItemId}")
    fun deleteTicketItem(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @PathVariable ticketItemId: Long,
    ): GetEventTicketItemsResponse = deleteTicketItemUseCase.execute(userId, eventId, ticketItemId)
}
