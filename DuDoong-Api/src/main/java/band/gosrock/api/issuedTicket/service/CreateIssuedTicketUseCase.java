package band.gosrock.api.issuedTicket.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDetailResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.dto.request.CreateIssuedTicketForDevDTO;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainDevService;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateIssuedTicketUseCase {

    private final IssuedTicketDomainService issuedTicketDomainService;
    private final IssuedTicketDomainDevService issuedTicketDomainDevService;
    private final IssuedTicketRepository issuedTicketRepository;
    private final UserAdaptor userAdaptor;

    public RetrieveIssuedTicketDetailResponse executeForDev(CreateIssuedTicketForDevDTO body) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userAdaptor.queryUser(currentUserId);
        IssuedTicket issuedTicket = issuedTicketDomainDevService.createIssuedTicket(body, user);
        return new RetrieveIssuedTicketDetailResponse(
                issuedTicket.toIssuedTicketInfoVo(),
                issuedTicket.getEvent().toEventInfoVo(),
                issuedTicket.getUser().getProfile().getName());
    }
}
