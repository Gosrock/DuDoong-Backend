package band.gosrock.admin.controller

import band.gosrock.admin.model.dto.request.AdminUpdateEventRequest
import band.gosrock.admin.model.dto.request.AdminUpdateEventStatusRequest
import band.gosrock.admin.model.dto.response.AdminEventResponse
import band.gosrock.admin.model.dto.response.AdminIssuedTicketResponse
import band.gosrock.admin.service.AdminDeleteEventUseCase
import band.gosrock.admin.service.AdminExcelService
import band.gosrock.admin.service.AdminGetEventDetailUseCase
import band.gosrock.admin.service.AdminGetEventsUseCase
import band.gosrock.admin.service.AdminGetIssuedTicketsUseCase
import band.gosrock.admin.service.AdminUpdateEventStatusUseCase
import band.gosrock.admin.service.AdminUpdateEventUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal-api/v1/events")
@SecurityRequirement(name = "admin-token")
@Tag(name = "Admin")
class AdminEventController(
    private val adminGetEventsUseCase: AdminGetEventsUseCase,
    private val adminGetEventDetailUseCase: AdminGetEventDetailUseCase,
    private val adminDeleteEventUseCase: AdminDeleteEventUseCase,
    private val adminUpdateEventStatusUseCase: AdminUpdateEventStatusUseCase,
    private val adminUpdateEventUseCase: AdminUpdateEventUseCase,
    private val adminGetIssuedTicketsUseCase: AdminGetIssuedTicketsUseCase,
    private val adminExcelService: AdminExcelService,
) {

    @Operation(summary = "이벤트 목록을 엑셀로 다운로드합니다.")
    @GetMapping("/export")
    fun exportEvents(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: String?,
    ): ResponseEntity<ByteArray> {
        val events = adminGetEventsUseCase.executeAll(keyword, status)
        val bytes = adminExcelService.generateEventsExcel(events)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=events.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(bytes)
    }

    @Operation(summary = "이벤트 목록을 조회합니다.")
    @GetMapping
    fun getEvents(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<AdminEventResponse> {
        return adminGetEventsUseCase.execute(keyword, status, pageable)
    }

    @Operation(summary = "이벤트 상세 정보를 조회합니다.")
    @GetMapping("/{eventId}")
    fun getEventDetail(@PathVariable eventId: Long): AdminEventResponse {
        return adminGetEventDetailUseCase.execute(eventId)
    }

    @Operation(summary = "이벤트를 소프트 삭제합니다.")
    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteEvent(@PathVariable eventId: Long) {
        adminDeleteEventUseCase.execute(eventId)
    }

    @Operation(summary = "이벤트 상태를 변경합니다. (어드민 전용, 밸리데이션 우회)")
    @PatchMapping("/{eventId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateEventStatus(
        @PathVariable eventId: Long,
        @RequestBody request: AdminUpdateEventStatusRequest,
    ) {
        adminUpdateEventStatusUseCase.execute(eventId, request)
    }

    @Operation(summary = "이벤트 정보를 수정합니다. (어드민 전용, OPEN 상태에서도 수정 가능)")
    @PatchMapping("/{eventId}")
    fun updateEvent(
        @PathVariable eventId: Long,
        @RequestBody request: AdminUpdateEventRequest,
    ): AdminEventResponse {
        return adminUpdateEventUseCase.execute(eventId, request)
    }

    @Operation(summary = "이벤트별 발급 티켓 목록을 조회합니다.")
    @GetMapping("/{eventId}/issued-tickets")
    fun getIssuedTickets(
        @PathVariable eventId: Long,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<AdminIssuedTicketResponse> {
        return adminGetIssuedTicketsUseCase.execute(eventId, pageable)
    }
}
