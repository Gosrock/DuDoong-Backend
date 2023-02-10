package band.gosrock.api.issuedTicket.controller;


import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketListResponse;
import band.gosrock.api.issuedTicket.service.EntranceIssuedTicketUseCase;
import band.gosrock.api.issuedTicket.service.ReadIssuedTicketsUseCase;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "8-1. 발급 티켓 관련 어드민 컨트롤러")
@RestController
@RequestMapping("/v1/events/{eventId}/issuedTickets")
@RequiredArgsConstructor
public class AdminIssuedTicketController {

    private final ReadIssuedTicketsUseCase readIssuedTicketsUseCase;

    private final EntranceIssuedTicketUseCase entranceIssuedTicketUseCase;

    @Operation(summary = "[어드민 기능] 발급 티켓 리스트 가져오기 API 입니다.")
    @GetMapping
    public RetrieveIssuedTicketListResponse getIssuedTickets(
            @PathVariable Long eventId,
            @RequestParam Long page,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String phoneNumber) {
        return readIssuedTicketsUseCase.execute(page, eventId, userName, phoneNumber);
    }

    @Operation(summary = "[어드민 기능] 발급 티켓 입장 처리 API 입니다.")
    @PatchMapping(value = "/{issuedTicketId}")
    public IssuedTicketInfoVo patchIssuedTicketStatus(
            @PathVariable Long eventId, @PathVariable Long issuedTicketId) {
        return entranceIssuedTicketUseCase.execute(eventId, issuedTicketId);
    }
}
