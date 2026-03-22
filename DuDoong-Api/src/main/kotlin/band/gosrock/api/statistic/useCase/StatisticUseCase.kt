package band.gosrock.api.statistic.useCase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.statistic.dto.DashBoardStatisticResponse
import band.gosrock.api.statistic.query.IssuedTicketQueryRepository
import band.gosrock.api.statistic.query.OrderQueryRepository
import band.gosrock.common.annotation.UseCase
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class StatisticUseCase(
    private val issuedTicketQueryRepository: IssuedTicketQueryRepository,
    private val orderQueryRepository: OrderQueryRepository,
) {
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long): DashBoardStatisticResponse =
        DashBoardStatisticResponse.of(
            orderQueryRepository.statistic(eventId),
            issuedTicketQueryRepository.statistic(eventId),
        )
}
