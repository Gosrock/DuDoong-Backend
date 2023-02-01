package band.gosrock.api.ticketItem.controller;


import band.gosrock.api.ticketItem.dto.request.CreateTicketOptionRequest;
import band.gosrock.api.ticketItem.dto.response.GetEventOptionsResponse;
import band.gosrock.api.ticketItem.dto.response.OptionGroupResponse;
import band.gosrock.api.ticketItem.service.CreateTicketOptionUseCase;
import band.gosrock.api.ticketItem.service.DeleteOptionGroupUseCase;
import band.gosrock.api.ticketItem.service.GetEventOptionsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "access-token")
@Tag(name = "티켓상품 옵션 관련 컨트롤러")
@RestController
@RequestMapping("/v1/event/{eventId}/ticketOptions")
@RequiredArgsConstructor
public class TicketOptionController {

    private final CreateTicketOptionUseCase createTicketOptionUseCase;
    private final GetEventOptionsUseCase getEventOptionsUseCase;
    private final DeleteOptionGroupUseCase deleteOptionGroupUseCase;

    @Operation(summary = "해당 이벤트에 속하는 티켓옵션을 생성합니다.")
    @PostMapping
    public OptionGroupResponse createTicketOption(
            @RequestBody @Valid CreateTicketOptionRequest createTicketOptionRequest,
            @PathVariable Long eventId) {
        return createTicketOptionUseCase.execute(createTicketOptionRequest, eventId);
    }

    @Operation(summary = "해당 이벤트에 속하는 옵션을 모두 조회합니다.")
    @GetMapping
    public GetEventOptionsResponse getEventOptions(@PathVariable Long eventId) {
        return getEventOptionsUseCase.execute(eventId);
    }

    @Operation(summary = "해당 옵션그룹을 삭제합니다.")
    @PatchMapping("/{optionGroupId}")
    public GetEventOptionsResponse deleteOptionGroup(
            @PathVariable Long eventId, @PathVariable Long optionGroupId) {
        return deleteOptionGroupUseCase.execute(eventId, optionGroupId);
    }
}
