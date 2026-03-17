package band.gosrock.admin.controller

import band.gosrock.admin.model.dto.response.AdminEventResponse
import band.gosrock.admin.service.AdminDeleteEventUseCase
import band.gosrock.admin.service.AdminGetEventDetailUseCase
import band.gosrock.admin.service.AdminGetEventsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
) {

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
}
