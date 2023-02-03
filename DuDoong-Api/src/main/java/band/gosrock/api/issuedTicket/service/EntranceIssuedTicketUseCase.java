package band.gosrock.api.issuedTicket.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER;

import band.gosrock.api.common.UserUtils;
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.issuedTicket.mapper.IssuedTicketMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class EntranceIssuedTicketUseCase {

    private final IssuedTicketDomainService issuedTicketDomainService;

    private final IssuedTicketMapper issuedTicketMapper;

    private final UserUtils userUtils;

    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    public IssuedTicketInfoVo execute(Long eventId, Long issuedTicketId) {
        return issuedTicketDomainService.processingEntranceIssuedTicket(eventId, issuedTicketId);
    }
}
