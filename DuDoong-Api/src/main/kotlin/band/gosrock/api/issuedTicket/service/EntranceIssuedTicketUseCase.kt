package band.gosrock.api.issuedTicket.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.common.vo.IssuedTicketInfoVo
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService
import org.springframework.transaction.annotation.Transactional

@UseCase
class EntranceIssuedTicketUseCase(
    private val issuedTicketDomainService: IssuedTicketDomainService,
) {

    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long, uuid: String): IssuedTicketInfoVo {
        return issuedTicketDomainService.processingEntranceIssuedTicket(eventId, uuid)
    }
}
