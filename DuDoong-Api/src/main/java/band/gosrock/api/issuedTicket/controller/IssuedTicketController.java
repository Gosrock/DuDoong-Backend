package band.gosrock.api.issuedTicket.controller;


import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDetailResponse;
import band.gosrock.api.issuedTicket.service.ReadIssuedTicketUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "8-2. [발급티켓]")
@RestController
@RequestMapping("/v1/issuedTickets")
@RequiredArgsConstructor
public class IssuedTicketController {

    private final ReadIssuedTicketUseCase readIssuedTicketUseCase;

    @Operation(summary = "발급 티켓 가져오기 API 입니다.")
    @GetMapping(value = "/{uuid}", produces = "application/json; charset=utf-8")
    public RetrieveIssuedTicketDetailResponse getIssuedTicket(@PathVariable String uuid) {
        return readIssuedTicketUseCase.execute(uuid);
    }
}
