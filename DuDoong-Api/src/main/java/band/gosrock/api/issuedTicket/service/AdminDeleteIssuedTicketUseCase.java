package band.gosrock.api.issuedTicket.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.issuedTicket.mapper.IssuedTicketMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import band.gosrock.domain.domains.issuedTicket.validator.IssuedTicketValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class AdminDeleteIssuedTicketUseCase {

    private final IssuedTicketDomainService issuedTicketDomainService;

    private final UserUtils userUtils;

    private final IssuedTicketMapper issuedTicketMapper;

    private final IssuedTicketValidator issuedTicketValidator;

    @Transactional
    public void execute(Long eventId, Long issuedTicketId) {
        Long currentUserId = userUtils.getCurrentUserId();
        IssuedTicket issuedTicket = issuedTicketMapper.getIssuedTicket(issuedTicketId);
        issuedTicketValidator.validIssuedTicketEventIdEqualEvent(issuedTicket, eventId);
        issuedTicketValidator.validCanModifyIssuedTicketUser(issuedTicket, currentUserId);
        issuedTicketDomainService.adminCancelIssuedTicket(
                issuedTicket, issuedTicket.getItemInfo().getTicketItemId());
    }
}
