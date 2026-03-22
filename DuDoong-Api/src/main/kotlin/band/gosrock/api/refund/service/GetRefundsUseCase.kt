package band.gosrock.api.refund.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.common.page.PageResponse
import band.gosrock.api.refund.dto.response.RefundResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class GetRefundsUseCase(
    private val orderAdaptor: OrderAdaptor,
    private val userAdaptor: UserAdaptor,
    private val eventAdaptor: EventAdaptor,
) {

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun execute(
        userId: Long,
        eventId: Long,
        refundStatus: RefundStatus?,
        pageable: Pageable,
    ): PageResponse<RefundResponse> {
        val orderPage = orderAdaptor.findRefunds(eventId, refundStatus, null, pageable)

        val userIds = orderPage.content.mapNotNull { it.userId }
        val userMap = userAdaptor.findUserByIdIn(userIds).associateBy { it.id }

        val eventName = runCatching { eventAdaptor.findById(eventId).eventBasic?.name }.getOrNull()

        return PageResponse.of(orderPage.map { order ->
            val userName = order.userId?.let { userMap[it]?.profile?.name }
            RefundResponse.of(order, userName, eventName)
        })
    }

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun getDetail(userId: Long, eventId: Long, orderUuid: String): RefundResponse {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        val userName = order.userId?.let {
            runCatching { userAdaptor.queryUser(it).profile?.name }.getOrNull()
        }
        val eventName = order.eventId?.let {
            runCatching { eventAdaptor.findById(it).eventBasic?.name }.getOrNull()
        }
        return RefundResponse.of(order, userName, eventName)
    }
}
