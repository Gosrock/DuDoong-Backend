package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminOrderResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.repository.OrderRepository
import band.gosrock.domain.domains.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetOrdersUseCase(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository,
) {

    fun executeAll(keyword: String?, status: OrderStatus?, eventId: Long?): List<AdminOrderResponse> {
        val orders = orderRepository.findAllForAdminNoPage(keyword, status, eventId)
        val userIds = orders.mapNotNull { it.userId }
        val eventIds = orders.mapNotNull { it.eventId }
        val userMap = userRepository.findAllByIdIn(userIds).associateBy { it.id }
        val eventMap = eventRepository.findAllByIdIn(eventIds).associateBy { it.id }
        return orders.map { order ->
            val userName = order.userId?.let { userMap[it]?.profile?.name }
            val eventName = order.eventId?.let { eventMap[it]?.eventBasic?.name }
            AdminOrderResponse.of(order, userName, eventName)
        }
    }

    fun execute(keyword: String?, status: OrderStatus?, eventId: Long?, pageable: Pageable): Page<AdminOrderResponse> {
        val orderPage = orderRepository.findAllForAdmin(keyword, status, eventId, pageable)

        // batch fetch users and events to avoid N+1
        val userIds = orderPage.content.mapNotNull { it.userId }
        val eventIds = orderPage.content.mapNotNull { it.eventId }

        val userMap = userRepository.findAllByIdIn(userIds).associateBy { it.id }
        val eventMap = eventRepository.findAllByIdIn(eventIds).associateBy { it.id }

        return orderPage.map { order ->
            val userName = order.userId?.let { userMap[it]?.profile?.name }
            val eventName = order.eventId?.let { eventMap[it]?.eventBasic?.name }
            AdminOrderResponse.of(order, userName, eventName)
        }
    }
}
