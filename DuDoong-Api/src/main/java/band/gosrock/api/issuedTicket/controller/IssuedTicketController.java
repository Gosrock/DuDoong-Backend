package band.gosrock.api.issuedTicket.controller;

import band.gosrock.api.issuedTicket.dto.request.PostIssuedTicketRequestDTOs;
import band.gosrock.api.issuedTicket.dto.response.PostIssuedTicketResponse;
import band.gosrock.api.issuedTicket.service.CreateIssuedTicketUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    @Operation(summary = "결제 승인 API 처리 이후 사용자가 입력한 정보 및 옵션들을 바탕으로 티켓을 발급합니다.")
    @PostMapping
    public PostIssuedTicketResponse createIssuedTicket(@RequestBody PostIssuedTicketRequestDTOs body) {
        return createIssuedTicketUseCase.execute(body);
    }
}
