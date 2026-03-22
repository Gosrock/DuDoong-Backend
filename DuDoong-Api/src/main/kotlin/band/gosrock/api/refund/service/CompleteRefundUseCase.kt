package band.gosrock.api.refund.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.refund.dto.response.RefundResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class CompleteRefundUseCase(
    private val orderAdaptor: OrderAdaptor,
    private val userAdaptor: UserAdaptor,
    private val eventAdaptor: EventAdaptor,
) {

    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(userId: Long, eventId: Long, orderUuid: String): RefundResponse {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        order.completeRefund()

        val userName = order.userId?.let {
            runCatching { userAdaptor.queryUser(it).profile?.name }.getOrNull()
        }
        val eventName = order.eventId?.let {
            runCatching { eventAdaptor.findById(it).eventBasic?.name }.getOrNull()
        }
        return RefundResponse.of(order, userName, eventName)
    }
}
