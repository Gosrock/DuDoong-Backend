package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.DashboardResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.repository.OrderRepository
import band.gosrock.domain.domains.user.repository.UserRepository
import java.time.LocalDate
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class GetDashboardUseCase(
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val eventRepository: EventRepository,
) {

    fun execute(): DashboardResponse {
        val todayStart = LocalDate.now().atStartOfDay()

        val totalUsers = userRepository.count()
        val todayNewUsers = userRepository.countByCreatedAtAfter(todayStart)
        val todayOrders = orderRepository.countByCreatedAtAfter(todayStart)

        val todayConfirmedOrders = orderRepository.findByCreatedAtAfterAndOrderStatusIn(
            todayStart,
            listOf(OrderStatus.CONFIRM, OrderStatus.APPROVED)
        )
        val todayRevenue = todayConfirmedOrders.sumOf {
            it.getTotalPaymentPrice().longValue()
        }

        val activeEvents = eventRepository.countByStatusNative(EventStatus.OPEN.statusName)

        val todayRefunds = orderRepository.countByCreatedAtAfterAndOrderStatus(
            todayStart,
            OrderStatus.REFUND
        )

        return DashboardResponse(
            totalUsers = totalUsers,
            todayNewUsers = todayNewUsers,
            todayOrders = todayOrders,
            todayRevenue = todayRevenue,
            activeEvents = activeEvents,
            todayRefunds = todayRefunds,
        )
    }
}
