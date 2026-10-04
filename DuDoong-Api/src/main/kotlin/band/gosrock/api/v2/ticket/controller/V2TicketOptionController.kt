package band.gosrock.api.v2.ticket.controller

import band.gosrock.api.v2.ticket.dto.request.V2CreateTicketOptionRequest
import band.gosrock.api.v2.ticket.dto.request.V2UpdateTicketOptionRequest
import band.gosrock.api.v2.ticket.dto.response.V2TicketOptionResponse
import band.gosrock.api.v2.ticket.usecase.V2CreateTicketOptionUseCase
import band.gosrock.api.v2.ticket.usecase.V2DeleteTicketOptionUseCase
import band.gosrock.api.v2.ticket.usecase.V2ReadTicketOptionsUseCase
import band.gosrock.api.v2.ticket.usecase.V2UpdateTicketOptionUseCase
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = "v2. 티켓 옵션")
@RestController
@RequestMapping("/api/v2/events/{eventId}/options")
class V2TicketOptionController(
    private val readTicketOptionsUseCase: V2ReadTicketOptionsUseCase,
    private val createTicketOptionUseCase: V2CreateTicketOptionUseCase,
    private val updateTicketOptionUseCase: V2UpdateTicketOptionUseCase,
    private val deleteTicketOptionUseCase: V2DeleteTicketOptionUseCase,
) {
    @Operation(summary = "[O-1] 공연 옵션 풀 (일반 멤버 이상). 적용 티켓 id·잠김 여부 포함")
    @GetMapping
    fun getOptions(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
    ): List<V2TicketOptionResponse> = readTicketOptionsUseCase.execute(userId, eventId)

    @Operation(summary = "[O-2] 옵션 생성 (매니저 이상). SUBJECTIVE / YES_NO")
    @PostMapping
    fun createOption(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @RequestBody @Valid request: V2CreateTicketOptionRequest,
    ): V2TicketOptionResponse = createTicketOptionUseCase.execute(userId, eventId, request)

    @Operation(summary = "[O-3] 옵션 수정 (매니저 이상). null 은 변경 안 함, 응답 형식 변경 불가. 판매된 티켓에 붙은 옵션은 이름·설명만")
    @PatchMapping("/{optionId}")
    fun updateOption(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @PathVariable optionId: Long,
        @RequestBody @Valid request: V2UpdateTicketOptionRequest,
    ): V2TicketOptionResponse = updateTicketOptionUseCase.execute(userId, eventId, optionId, request)

    @Operation(summary = "[O-4] 옵션 삭제 (매니저 이상). 판매된 티켓에 붙어 있으면 불가. 남은 옵션 목록 반환")
    @DeleteMapping("/{optionId}")
    fun deleteOption(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @PathVariable optionId: Long,
    ): List<V2TicketOptionResponse> = deleteTicketOptionUseCase.execute(userId, eventId, optionId)
}
