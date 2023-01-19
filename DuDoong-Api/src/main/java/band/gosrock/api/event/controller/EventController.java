package band.gosrock.api.event.controller;


import band.gosrock.api.event.model.dto.request.CreateEventRequest;
import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.api.event.service.CreateEventUseCase;
import band.gosrock.api.event.service.ReadEventUseCase;
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

    private final CreateEventUseCase createEventUseCase;
    private final ReadEventUseCase readEventUseCase;

    @Operation(summary = "내가 관리하는 이벤트 리스트를 가져옵니다")
    @GetMapping
    public List<EventResponse> getAllEvents() {
        return readEventUseCase.execute();
    }

    @Operation(summary = "새로운 이벤트(공연)를 생성합니다")
    @PostMapping("/{hostId}")
    public EventResponse createEvent(
            @PathVariable Long hostId, @RequestBody @Valid CreateEventRequest createEventRequest) {
        return createEventUseCase.execute(hostId, createEventRequest);
    }
}
