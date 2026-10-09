package band.gosrock.api.v2.event.controller

import band.gosrock.api.v2.common.swagger.V2ApiTags
import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.event.dto.request.V2CreateEventRequest
import band.gosrock.api.v2.event.dto.request.V2EventImageUploadRequest
import band.gosrock.api.v2.event.dto.request.V2UpdateEventSectionsRequest
import band.gosrock.api.v2.event.dto.request.V2UpdateEventBasicRequest
import band.gosrock.api.v2.event.dto.response.V2CreateEventResponse
import band.gosrock.api.v2.event.dto.response.V2EventChecklistResponse
import band.gosrock.api.v2.event.dto.response.V2EventImageUploadResponse
import band.gosrock.api.v2.event.dto.response.V2EventManageResponse
import band.gosrock.api.v2.event.dto.response.V2EventSectionResponse
import band.gosrock.api.v2.event.dto.response.V2EventStatusResponse
import band.gosrock.api.v2.event.dto.response.V2MyEventResponse
import band.gosrock.api.v2.event.usecase.V2CreateEventUseCase
import band.gosrock.api.v2.event.usecase.V2DeleteEventUseCase
import band.gosrock.api.v2.event.usecase.V2GetEventImageUploadUrlUseCase
import band.gosrock.api.v2.event.usecase.V2OpenEventUseCase
import band.gosrock.api.v2.event.usecase.V2ReadEventChecklistUseCase
import band.gosrock.api.v2.event.usecase.V2ReadEventManageUseCase
import band.gosrock.api.v2.event.usecase.V2ReadEventSectionsUseCase
import band.gosrock.api.v2.event.usecase.V2ReadMyEventsUseCase
import band.gosrock.api.v2.event.usecase.V2UpdateEventBasicUseCase
import band.gosrock.api.v2.event.usecase.V2UpdateEventSectionsUseCase
import band.gosrock.common.annotation.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = V2ApiTags.EVENT_PREP, description = V2ApiTags.EVENT_PREP_DESCRIPTION)
@RestController
@RequestMapping("/api/v2")
@Validated
class V2EventController(
    private val readMyEventsUseCase: V2ReadMyEventsUseCase,
    private val createEventUseCase: V2CreateEventUseCase,
    private val readEventManageUseCase: V2ReadEventManageUseCase,
    private val updateEventBasicUseCase: V2UpdateEventBasicUseCase,
    private val readEventSectionsUseCase: V2ReadEventSectionsUseCase,
    private val updateEventSectionsUseCase: V2UpdateEventSectionsUseCase,
    private val readEventChecklistUseCase: V2ReadEventChecklistUseCase,
    private val openEventUseCase: V2OpenEventUseCase,
    private val deleteEventUseCase: V2DeleteEventUseCase,
    private val getEventImageUploadUrlUseCase: V2GetEventImageUploadUrlUseCase,
) {
    @Operation(summary = "[E-1] 내 호스트들(활성 멤버)의 공연 목록. keyword 는 공연명 부분일치, 최신 생성 순")
    @GetMapping("/me/events")
    fun getMyEvents(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) size: Int,
    ): V2PageResponse<V2MyEventResponse> = readMyEventsUseCase.execute(userId, keyword, page, size)

    @Operation(summary = "[E-2] 간편 공연 만들기 (hostId 의 매니저 이상). 상태는 준비중")
    @PostMapping("/events")
    fun createEvent(
        @CurrentUserId userId: Long,
        @RequestBody @Valid request: V2CreateEventRequest,
    ): V2CreateEventResponse = createEventUseCase.execute(userId, request.hostId!!, request)

    @Operation(summary = "[E-3] 어드민용 공연 전체 정보 (일반 멤버 이상)")
    @GetMapping("/events/{eventId}/manage")
    fun getEventManage(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
    ): V2EventManageResponse = readEventManageUseCase.execute(userId, eventId)

    @Operation(summary = "[E-4] 기본 정보 수정 (매니저 이상). null 은 변경 안 함, 등록 후에도 가능 (hasTicket 은 준비중만)")
    @PatchMapping("/events/{eventId}/basic")
    fun updateEventBasic(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @RequestBody @Valid request: V2UpdateEventBasicRequest,
    ): V2EventManageResponse = updateEventBasicUseCase.execute(userId, eventId, request)

    @Operation(summary = "[E-5] 상세 정보 섹션 목록 (비로그인 허용, 준비중 공연은 멤버만 — 아니면 404)")
    @GetMapping("/events/{eventId}/sections")
    fun getEventSections(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
    ): List<V2EventSectionResponse> = readEventSectionsUseCase.execute(userId, eventId)

    @Operation(summary = "[E-6] 섹션 전체 저장 (매니저 이상). 본문 {sections: [...]}, 1~10개 (#755: 맨 배열 → 객체)")
    @PutMapping("/events/{eventId}/sections")
    fun updateEventSections(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @RequestBody request: V2UpdateEventSectionsRequest,
    ): List<V2EventSectionResponse> = updateEventSectionsUseCase.execute(userId, eventId, request.sections.orEmpty())

    @Operation(summary = "[E-7] 등록 체크리스트 (일반 멤버 이상). hasTicket=false 면 티켓 면제")
    @GetMapping("/events/{eventId}/checklist")
    fun getEventChecklist(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
    ): V2EventChecklistResponse = readEventChecklistUseCase.execute(userId, eventId)

    @Operation(summary = "[E-8] 공연 등록(공개) (매니저 이상). 체크리스트 충족 필요")
    @PostMapping("/events/{eventId}/open")
    fun openEvent(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
    ): V2EventStatusResponse = openEventUseCase.execute(userId, eventId)

    @Operation(summary = "[E-9] 공연 삭제 (매니저 이상). 준비중 공연만")
    @DeleteMapping("/events/{eventId}")
    fun deleteEvent(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
    ): V2EventStatusResponse = deleteEventUseCase.execute(userId, eventId)

    @Operation(summary = "[E-10] 포스터 / 본문 이미지 업로드 url 발급 (매니저 이상)")
    @PostMapping("/events/{eventId}/images")
    fun getImageUploadUrl(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @RequestBody @Valid request: V2EventImageUploadRequest,
    ): V2EventImageUploadResponse = getEventImageUploadUrlUseCase.execute(userId, eventId, request)

    companion object {
        private const val MAX_PAGE_SIZE = 50L
    }
}
