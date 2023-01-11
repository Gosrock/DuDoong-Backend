package band.gosrock.api.issuedTicket.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDetailResponse;
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketListResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.issuedTicket.dto.response.IssuedTicketDTO;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReadIssuedTicketUseCase {

    private final IssuedTicketDomainService issuedTicketDomainService;
    private final UserDomainService userDomainService;

    /**
     * 발급 티켓 상세 정보 API
     *
     * @param issuedTicketId 발급 티켓 id
     * @return RetrieveIssuedTicketDetailResponse
     */
    public RetrieveIssuedTicketDetailResponse execute(Long issuedTicketId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        /*
        Todo: 지연 로딩 트랜잭션 이슈 때문에 일단은 DTO로 감싸서 올림. But, 이게 최선일까? 고민해보자.
         */
        IssuedTicketDTO issuedTicket =
                issuedTicketDomainService.retrieveIssuedTicket(currentUserId, issuedTicketId);
        User user = userDomainService.retrieveUser(currentUserId);
        return new RetrieveIssuedTicketDetailResponse(issuedTicket, user);
    }
}
