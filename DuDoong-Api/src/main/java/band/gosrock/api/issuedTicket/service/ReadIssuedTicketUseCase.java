package band.gosrock.api.issuedTicket.service;

import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDetailResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ReadIssuedTicketUseCase {

    private final IssuedTicketDomainService issuedTicketDomainService;
    private final UserDomainService userDomainService;

    /**
     * 발급 티켓 상세 정보 API
     * @param issuedTicketId 발급 티켓 id
     * @return RetrieveIssuedTicketDetailResponse
     */
    public RetrieveIssuedTicketDetailResponse execute(Long issuedTicketId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        IssuedTicket issuedTicket = issuedTicketDomainService.retrieveIssuedTicket(currentUserId,
            issuedTicketId);
        User user = userDomainService.retrieveUser(currentUserId);
        return new RetrieveIssuedTicketDetailResponse(issuedTicket, user);
    }
}
