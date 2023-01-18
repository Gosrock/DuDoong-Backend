package band.gosrock.api.issuedTicket.controller;


import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDetailResponse;
import band.gosrock.api.issuedTicket.service.CreateIssuedTicketUseCase;
import band.gosrock.api.issuedTicket.service.ReadIssuedTicketUseCase;
import band.gosrock.common.annotation.DevelopOnlyApi;
import band.gosrock.domain.domains.issuedTicket.dto.request.CreateIssuedTicketForDevDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "발급 티켓 관련 컨트롤러")
@RestController
@RequestMapping("/v1/issuedTickets")
@RequiredArgsConstructor
public class IssuedTicketController {

    private final CreateIssuedTicketUseCase createIssuedTicketUseCase;
    private final ReadIssuedTicketUseCase readIssuedTicketUseCase;

    @Operation(summary = "발급 티켓 가져오기 API 입니다.")
    @GetMapping(value = "/{issuedTicketId}", produces = "application/json; charset=utf-8")
    public RetrieveIssuedTicketDetailResponse getIssuedTicket(@PathVariable Long issuedTicketId) {
        return readIssuedTicketUseCase.execute(issuedTicketId);
    }

    @Operation(summary = "개발용 발급 티켓 생성 API 입니다.")
    @DevelopOnlyApi
    @PostMapping(value = "/develop")
    public RetrieveIssuedTicketDetailResponse postIssuedTicket(
            @RequestBody CreateIssuedTicketForDevDTO body) {
        return createIssuedTicketUseCase.executeForDev(body);
    }
}
