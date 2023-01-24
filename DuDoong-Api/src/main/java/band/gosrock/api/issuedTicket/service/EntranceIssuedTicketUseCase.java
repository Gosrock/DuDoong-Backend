package band.gosrock.api.issuedTicket.service;


import band.gosrock.api.common.UserUtils;
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

    public IssuedTicketInfoVo execute(Long issuedTicketId) {
        Long currentUserId = userUtils.getCurrentUserId();
        return issuedTicketDomainService.processingEntranceIssuedTicket(
                currentUserId, issuedTicketId);
    }
}
