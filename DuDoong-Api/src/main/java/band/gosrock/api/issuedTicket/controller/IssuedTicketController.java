package band.gosrock.api.issuedTicket.controller;


import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDetailResponse;
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketListResponse;
import band.gosrock.api.issuedTicket.service.CreateIssuedTicketUseCase;
import band.gosrock.api.issuedTicket.service.ReadIssuedTicketUseCase;
import band.gosrock.api.issuedTicket.service.ReadIssuedTicketsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "발급 티켓 관련 컨트롤러")
@RestController
@RequestMapping("/v1/issuedTickets")
@RequiredArgsConstructor
public class IssuedTicketController {

    private final CreateIssuedTicketUseCase createIssuedTicketUseCase;
    private final ReadIssuedTicketUseCase readIssuedTicketUseCase;
    private final ReadIssuedTicketsUseCase readIssuedTicketsUseCase;

    @Operation(summary = "발급 티켓 가져오기 API 입니다.")
    @GetMapping(value = "/{issuedTicketId}", produces = "application/json; charset=utf-8")
    public RetrieveIssuedTicketDetailResponse getIssuedTicket(@PathVariable Long issuedTicketId) {
        return readIssuedTicketUseCase.execute(issuedTicketId);
    }

    @Operation(summary = "[어드민 기능] 발급 티켓 리스트 가져오기 API 입니다.")
    @GetMapping
    public RetrieveIssuedTicketListResponse getIssuedTickets(
            @RequestParam Long page,
            @RequestParam Long eventId,
            @RequestParam(required = false) String userName) {
        return readIssuedTicketsUseCase.execute(page, eventId, userName);
    }
}
