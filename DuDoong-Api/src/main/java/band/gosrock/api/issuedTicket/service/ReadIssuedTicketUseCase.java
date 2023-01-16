package band.gosrock.api.issuedTicket.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDetailResponse;
import band.gosrock.api.issuedTicket.mapper.IssuedTicketMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ReadIssuedTicketUseCase {

    private final IssuedTicketDomainService issuedTicketDomainService;
    private final IssuedTicketMapper issuedTicketMapper;

    /**
     * 발급 티켓 상세 정보 API
     *
     * @param issuedTicketId 발급 티켓 id
     * @return RetrieveIssuedTicketDetailResponse
     */
    public RetrieveIssuedTicketDetailResponse execute(Long issuedTicketId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return issuedTicketMapper.toIssuedTicketDetailResponse(currentUserId, issuedTicketId);
    }
}
