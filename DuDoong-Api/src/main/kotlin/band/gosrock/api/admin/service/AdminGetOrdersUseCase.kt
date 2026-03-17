package band.gosrock.api.admin.service

import band.gosrock.api.admin.model.dto.response.AdminOrderResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.repository.OrderRepository
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetOrdersUseCase(
    private val orderRepository: OrderRepository,
    private val userAdaptor: UserAdaptor,
    private val eventAdaptor: EventAdaptor,
) {

    fun execute(keyword: String?, status: OrderStatus?, pageable: Pageable): Page<AdminOrderResponse> {
        return orderRepository.findAllForAdmin(keyword, status, pageable)
            .map { order ->
                val userName = order.userId?.let {
                    runCatching { userAdaptor.queryUser(it).profile?.name }.getOrNull()
                }
                val eventName = order.eventId?.let {
                    runCatching { eventAdaptor.findById(it).eventBasic?.name }.getOrNull()
                }
                AdminOrderResponse.of(order, userName, eventName)
            }
    }
}
