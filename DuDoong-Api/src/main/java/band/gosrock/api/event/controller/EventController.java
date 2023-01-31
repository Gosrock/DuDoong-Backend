package band.gosrock.api.event.controller;


import band.gosrock.api.common.page.PageResponse;
import band.gosrock.api.event.model.dto.request.CreateEventRequest;
import band.gosrock.api.event.model.dto.request.UpdateEventBasicRequest;
import band.gosrock.api.event.model.dto.request.UpdateEventDetailRequest;
import band.gosrock.api.event.model.dto.response.EventProfileResponse;
import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.api.event.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "access-token")
@Tag(name = "이벤트(공연) 관련 컨트롤러")
@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final ReadEventProfileListUseCase readHostEventListUseCase;
    private final ReadUserEventProfilesUseCase readUserHostEventListUseCase;
    private final CreateEventUseCase createEventUseCase;
    private final UpdateEventBasicUseCase updateEventBasicUseCase;
    private final UpdateEventDetailUseCase updateEventDetailUseCase;

    @Operation(summary = "자신이 관리 중인 이벤트 리스트를 가져옵니다.")
    @GetMapping
    public PageResponse<EventProfileResponse> getAllEventByUser(
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return readUserHostEventListUseCase.execute(pageable);
    }

    // todo :: 호스트로 기능 빼기
    @Operation(summary = "특정 호스트가 관리 중인 이벤트 리스트를 가져옵니다.")
    @GetMapping("/host/{hostId}")
    public PageResponse<EventProfileResponse> getAllEventByHostId(
            @PathVariable Long hostId,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return readHostEventListUseCase.execute(hostId, pageable);
    }

    @Operation(summary = "공연 기본 정보를 등록하여, 새로운 이벤트(공연)를 생성합니다")
    @PostMapping
    public EventResponse createEvent(@RequestBody @Valid CreateEventRequest createEventRequest) {
        return createEventUseCase.execute(createEventRequest);
    }

    @Operation(summary = "공연 기본 정보를 등록하여, 새로운 이벤트(공연)를 생성합니다")
    @PatchMapping("/{eventId}/basic")
    public EventResponse updateEventBasic(
            @PathVariable Long eventId,
            @RequestBody @Valid UpdateEventBasicRequest updateEventBasicRequest) {
        return updateEventBasicUseCase.execute(eventId, updateEventBasicRequest);
    }

    @Operation(summary = "공연 상세 정보를 등록합니다.")
    @PatchMapping("/{eventId}/details")
    public EventResponse updateEventDetail(
            @PathVariable Long eventId,
            @RequestBody @Valid UpdateEventDetailRequest updateEventDetailRequest) {
        return updateEventDetailUseCase.execute(eventId, updateEventDetailRequest);
    }
}
