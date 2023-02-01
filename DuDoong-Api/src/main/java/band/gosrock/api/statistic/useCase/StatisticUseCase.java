package band.gosrock.api.statistic.useCase;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.GUEST;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.statistic.dto.DashBoardStatisticResponse;
import band.gosrock.api.statistic.query.IssuedTicketQueryRepository;
import band.gosrock.api.statistic.query.OrderQueryRepository;
import band.gosrock.common.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticUseCase {

    private final IssuedTicketQueryRepository issuedTicketQueryRepository;
    private final OrderQueryRepository orderQueryRepository;

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    public DashBoardStatisticResponse execute(Long eventId) {
        return DashBoardStatisticResponse.of(
                orderQueryRepository.statistic(eventId),
                issuedTicketQueryRepository.statistic(eventId));
    }
}
