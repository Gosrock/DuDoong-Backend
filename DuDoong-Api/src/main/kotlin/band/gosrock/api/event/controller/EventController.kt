package band.gosrock.api.event.controller

import band.gosrock.api.common.slice.SliceResponse
import band.gosrock.api.event.model.dto.request.CreateEventRequest
import band.gosrock.api.event.model.dto.request.UpdateEventBasicRequest
import band.gosrock.api.event.model.dto.request.UpdateEventDetailRequest
import band.gosrock.api.event.model.dto.request.UpdateEventStatusRequest
import band.gosrock.api.event.model.dto.response.EventChecklistResponse
import band.gosrock.api.event.model.dto.response.EventDetailResponse
import band.gosrock.api.event.model.dto.response.EventProfileResponse
import band.gosrock.api.event.model.dto.response.EventResponse
import band.gosrock.api.event.service.CreateEventUseCase
import band.gosrock.api.event.service.DeleteEventUseCase
import band.gosrock.api.event.service.OpenEventUseCase
import band.gosrock.api.event.service.ReadEventChecklistUseCase
import band.gosrock.api.event.service.ReadEventDetailUseCase
import band.gosrock.api.event.service.ReadUserEventProfilesUseCase
import band.gosrock.api.event.service.SearchEventsUseCase
import band.gosrock.api.event.service.UpdateEventBasicUseCase
import band.gosrock.api.event.service.UpdateEventDetailUseCase
import band.gosrock.api.event.service.UpdateEventStatusUseCase
import band.gosrock.common.annotation.CurrentUserId
import band.gosrock.common.annotation.DisableSwaggerSecurity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = "3. [이벤트(공연)]")
@RestController
@RequestMapping("/api/v1/events")
class EventController(
    private val readUserHostEventListUseCase: ReadUserEventProfilesUseCase,
    private val readEventDetailUseCase: ReadEventDetailUseCase,
    private val readEventChecklistUseCase: ReadEventChecklistUseCase,
    private val searchEventsUseCase: SearchEventsUseCase,
    private val createEventUseCase: CreateEventUseCase,
    private val updateEventBasicUseCase: UpdateEventBasicUseCase,
    private val updateEventDetailUseCase: UpdateEventDetailUseCase,
    private val updateEventStatusUseCase: UpdateEventStatusUseCase,
    private val openEventUseCase: OpenEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase
) {
    @Operation(summary = "자신이 관리 중인 이벤트 리스트를 가져옵니다.")
    @GetMapping
    fun getAllEventByUser(
        @CurrentUserId userId: Long,
        @ParameterObject @PageableDefault(size = 10) pageable: Pageable
    ): SliceResponse<EventProfileResponse> {
        return readUserHostEventListUseCase.execute(userId, pageable)
    }

    @Operation(summary = "이벤트 이름을 키워드로 검색하여 최신순으로 가져옵니다.")
    @DisableSwaggerSecurity
    @GetMapping("/search")
    fun getAllOpenEventByUser(
        @RequestParam(required = false) keyword: String?,
        @ParameterObject @PageableDefault(size = 10) pageable: Pageable
    ): SliceResponse<EventResponse> {
        return searchEventsUseCase.execute(keyword, pageable)
    }

    @Operation(summary = "공연 기본 정보를 등록하여, 새로운 이벤트(공연)를 생성합니다")
    @PostMapping
    fun createEvent(@CurrentUserId userId: Long, @RequestBody @Valid createEventRequest: CreateEventRequest): EventResponse {
        return createEventUseCase.execute(userId, createEventRequest)
    }

    @Operation(summary = "공연 상세 정보를 가져옵니다.")
    @DisableSwaggerSecurity
    @GetMapping("/{eventId}")
    fun getEventDetailById(@CurrentUserId userId: Long, @PathVariable eventId: Long): EventDetailResponse {
        return readEventDetailUseCase.execute(userId, eventId)
    }

    @Operation(summary = "공연 체크리스트 가져오기")
    @GetMapping("/{eventId}/checklist")
    fun getEventChecklistById(@PathVariable eventId: Long): EventChecklistResponse {
        return readEventChecklistUseCase.execute(eventId)
    }

    @Operation(summary = "공연 기본 정보를 등록하여, 새로운 이벤트(공연)를 생성합니다")
    @PatchMapping("/{eventId}/basic")
    fun updateEventBasic(
        @PathVariable eventId: Long,
        @RequestBody @Valid updateEventBasicRequest: UpdateEventBasicRequest
    ): EventResponse {
        return updateEventBasicUseCase.execute(eventId, updateEventBasicRequest)
    }

    @Operation(summary = "공연 상세 정보를 등록합니다.")
    @PatchMapping("/{eventId}/details")
    fun updateEventDetail(
        @PathVariable eventId: Long,
        @RequestBody @Valid updateEventDetailRequest: UpdateEventDetailRequest
    ): EventResponse {
        return updateEventDetailUseCase.execute(eventId, updateEventDetailRequest)
    }

    @Operation(summary = "공연을 오픈 상태로 변경합니다. 모든 체크리스트를 달성해야 합니다.")
    @PatchMapping("/{eventId}/open")
    fun updateEventOpen(@PathVariable eventId: Long): EventResponse {
        return openEventUseCase.execute(eventId)
    }

    @Operation(summary = "공연 상태를 변경합니다. (OPEN 제외)")
    @PatchMapping("/{eventId}/status")
    fun updateEventStatus(
        @PathVariable eventId: Long,
        @RequestBody @Valid updateEventDetailRequest: UpdateEventStatusRequest
    ): EventResponse {
        return updateEventStatusUseCase.execute(eventId, updateEventDetailRequest)
    }

    @Operation(summary = "공연을 삭제합니다. 조건에 맞지 않을 경우 삭제할 수 없습니다.")
    @PatchMapping("/{eventId}/delete")
    fun deleteEvent(@PathVariable eventId: Long): EventResponse {
        return deleteEventUseCase.execute(eventId)
    }
}
