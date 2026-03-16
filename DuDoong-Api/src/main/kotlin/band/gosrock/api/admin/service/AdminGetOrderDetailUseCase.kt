package band.gosrock.api.admin.service

import band.gosrock.api.admin.model.dto.response.AdminOrderResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetOrderDetailUseCase(
    private val orderAdaptor: OrderAdaptor,
    private val userAdaptor: UserAdaptor,
    private val eventAdaptor: EventAdaptor,
) {

    fun execute(orderUuid: String): AdminOrderResponse {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        val userName = order.userId?.let {
            runCatching { userAdaptor.queryUser(it).profile?.name }.getOrNull()
        }
        val eventName = order.eventId?.let {
            runCatching { eventAdaptor.findById(it).eventBasic?.name }.getOrNull()
        }
        return AdminOrderResponse.of(order, userName, eventName)
    }
}
