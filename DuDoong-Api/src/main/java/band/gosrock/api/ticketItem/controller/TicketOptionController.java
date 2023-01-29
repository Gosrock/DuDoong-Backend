package band.gosrock.api.ticketItem.controller;


import band.gosrock.api.ticketItem.dto.request.CreateTicketOptionRequest;
import band.gosrock.api.ticketItem.dto.response.CreateTicketOptionResponse;
import band.gosrock.api.ticketItem.service.CreateTicketOptionUseCase;
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

    @Operation(summary = "특정 이벤트에 속하는 티켓옵션을 생성합니다.")
    @PostMapping
    public CreateTicketOptionResponse createTicketOption(
            @RequestBody @Valid CreateTicketOptionRequest createTicketOptionRequest,
            @PathVariable Long eventId) {
        return createTicketOptionUseCase.execute(createTicketOptionRequest, eventId);
    }
}
