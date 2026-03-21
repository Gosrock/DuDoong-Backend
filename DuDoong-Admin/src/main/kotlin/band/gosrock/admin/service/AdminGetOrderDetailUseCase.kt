package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminOrderResponse
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
    private val adminAuthValidator: AdminAuthValidator,
) {

    fun execute(userId: Long, orderUuid: String): AdminOrderResponse {
        adminAuthValidator.validateManagerOrAbove(userId)
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
