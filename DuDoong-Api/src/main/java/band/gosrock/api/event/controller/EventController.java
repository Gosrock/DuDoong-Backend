package band.gosrock.api.event.controller;


import band.gosrock.api.event.model.dto.request.CreateEventRequest;
import band.gosrock.api.event.model.dto.request.UpdateEventBasicRequest;
import band.gosrock.api.event.model.dto.request.UpdateEventDetailRequest;
import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.api.event.service.CreateEventUseCase;
import band.gosrock.api.event.service.ReadEventListUseCase;
import band.gosrock.api.event.service.UpdateEventBasicUseCase;
import band.gosrock.api.event.service.UpdateEventDetailUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "access-token")
@Tag(name = "이벤트(공연) 관련 컨트롤러")
@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final ReadEventListUseCase readHostEventListUseCase;
    private final CreateEventUseCase createEventUseCase;
    private final UpdateEventBasicUseCase updateEventBasicUseCase;
    private final UpdateEventDetailUseCase updateEventDetailUseCase;

    // todo :: querydsl + 검색 기능 작동하도록 만들기
    @Operation(summary = "특정 호스트가 관리 중인 이벤트 리스트를 가져옵니다")
    @GetMapping
    public List<EventResponse> getAllEventByHostId(@RequestParam Long hostId) {
        return readHostEventListUseCase.execute(hostId);
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
