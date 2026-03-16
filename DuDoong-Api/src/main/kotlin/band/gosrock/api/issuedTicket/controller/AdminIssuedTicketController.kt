package band.gosrock.api.issuedTicket.controller

import band.gosrock.api.common.page.PageResponse
import band.gosrock.api.issuedTicket.dto.request.AdminIssuedTicketTableQueryRequest
import band.gosrock.api.issuedTicket.dto.response.IssuedTicketAdminTableElement
import band.gosrock.api.issuedTicket.service.EntranceIssuedTicketUseCase
import band.gosrock.api.issuedTicket.service.ReadIssuedTicketsUseCase
import band.gosrock.domain.common.vo.IssuedTicketInfoVo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = "8-1. [이벤트관리] 발급 티켓 관리 ")
@RestController
@RequestMapping("/v1/events/{eventId}/issuedTickets")
class AdminIssuedTicketController(
    private val readIssuedTicketsUseCase: ReadIssuedTicketsUseCase,
    private val entranceIssuedTicketUseCase: EntranceIssuedTicketUseCase,
) {

    @Operation(summary = "[어드민 기능] 발급 티켓 리스트 가져오기 API 입니다.")
    @GetMapping
    fun getIssuedTickets(
        @PathVariable eventId: Long,
        @ParameterObject queryRequest: AdminIssuedTicketTableQueryRequest,
        @ParameterObject pageable: Pageable,
    ): PageResponse<IssuedTicketAdminTableElement> {
        return readIssuedTicketsUseCase.execute(pageable, eventId, queryRequest)
    }

    @Operation(summary = "[어드민 기능] 발급 티켓 입장 처리 API 입니다.")
    @PatchMapping(value = ["/{uuid}"])
    fun patchIssuedTicketStatus(
        @PathVariable eventId: Long,
        @PathVariable uuid: String,
    ): IssuedTicketInfoVo {
        return entranceIssuedTicketUseCase.execute(eventId, uuid)
    }
}
