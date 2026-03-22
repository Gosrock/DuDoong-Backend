package band.gosrock.api.ticketItem.controller

import band.gosrock.api.ticketItem.dto.request.CreateTicketOptionRequest
import band.gosrock.api.ticketItem.dto.response.GetEventOptionsResponse
import band.gosrock.api.ticketItem.dto.response.OptionGroupResponse
import band.gosrock.api.ticketItem.service.CreateTicketOptionUseCase
import band.gosrock.api.ticketItem.service.DeleteOptionGroupUseCase
import band.gosrock.api.ticketItem.service.GetEventOptionsUseCase
import band.gosrock.common.annotation.CurrentUserId
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
@Tag(name = "7-2. [티켓상품옵션]")
@RestController
@RequestMapping("/api/v1/events/{eventId}/ticketOptions")
class TicketOptionController(
    private val createTicketOptionUseCase: CreateTicketOptionUseCase,
    private val getEventOptionsUseCase: GetEventOptionsUseCase,
    private val deleteOptionGroupUseCase: DeleteOptionGroupUseCase,
) {

    @Operation(summary = "해당 이벤트에 속하는 티켓옵션을 생성합니다.")
    @PostMapping
    fun createTicketOption(
        @CurrentUserId userId: Long,
        @RequestBody @Valid createTicketOptionRequest: CreateTicketOptionRequest,
        @PathVariable eventId: Long,
    ): OptionGroupResponse = createTicketOptionUseCase.execute(userId, createTicketOptionRequest, eventId)

    @Operation(summary = "해당 이벤트에 속하는 옵션을 모두 조회합니다.")
    @GetMapping
    fun getEventOptions(@PathVariable eventId: Long): GetEventOptionsResponse =
        getEventOptionsUseCase.execute(eventId)

    @Operation(summary = "해당 옵션그룹을 삭제합니다.")
    @PatchMapping("/{optionGroupId}")
    fun deleteOptionGroup(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @PathVariable optionGroupId: Long,
    ): GetEventOptionsResponse = deleteOptionGroupUseCase.execute(userId, eventId, optionGroupId)
}
