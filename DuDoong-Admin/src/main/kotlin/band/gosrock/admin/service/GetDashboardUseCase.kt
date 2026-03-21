package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminEventResponse
import band.gosrock.admin.model.dto.response.AdminOrderResponse
import band.gosrock.admin.model.dto.response.DashboardResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.repository.OrderRepository
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.repository.UserRepository
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class GetDashboardUseCase(
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val eventRepository: EventRepository,
    private val userAdaptor: UserAdaptor,
    private val hostAdaptor: HostAdaptor,
    private val adminAuthValidator: AdminAuthValidator,
) {

    fun execute(userId: Long, startDate: LocalDate? = null, endDate: LocalDate? = null): DashboardResponse {
        adminAuthValidator.validateManagerOrAbove(userId)
        val totalUsers = userRepository.count()
        val activeEvents = eventRepository.countByStatusNative(EventStatus.OPEN.statusName)

        val periodNewUsers: Long
        val periodOrders: Long
        val periodRevenue: Long
        val periodRefunds: Long

        if (startDate != null && endDate != null) {
            val start: LocalDateTime = startDate.atStartOfDay()
            val end: LocalDateTime = endDate.plusDays(1).atStartOfDay()

            periodNewUsers = userRepository.countByCreatedAtBetween(start, end)
            periodOrders = orderRepository.countByCreatedAtBetween(start, end)
            periodRefunds = orderRepository.countByCreatedAtBetweenAndOrderStatus(start, end, OrderStatus.REFUND)
            val confirmedOrders = orderRepository.findByCreatedAtBetweenAndOrderStatusIn(
                start, end,
                listOf(OrderStatus.CONFIRM, OrderStatus.APPROVED)
            )
            periodRevenue = confirmedOrders.sumOf { it.getTotalPaymentPrice().longValue() }
        } else {
            val todayStart: LocalDateTime = LocalDate.now().atStartOfDay()

            periodNewUsers = userRepository.countByCreatedAtAfter(todayStart)
            periodOrders = orderRepository.countByCreatedAtAfter(todayStart)
            periodRefunds = orderRepository.countByCreatedAtAfterAndOrderStatus(todayStart, OrderStatus.REFUND)
            val confirmedOrders = orderRepository.findByCreatedAtAfterAndOrderStatusIn(
                todayStart,
                listOf(OrderStatus.CONFIRM, OrderStatus.APPROVED)
            )
            periodRevenue = confirmedOrders.sumOf { it.getTotalPaymentPrice().longValue() }
        }

        // 최근 주문 5건
        val recentOrders = orderRepository.findTopNByOrderByCreatedAtDesc(PageRequest.of(0, 5))
            .map { order ->
                val userName = order.userId?.let {
                    runCatching { userAdaptor.queryUser(it).profile?.name }.getOrNull()
                }
                val eventName = order.eventId?.let {
                    runCatching { eventRepository.findByIdForAdmin(it)?.eventBasic?.name }.getOrNull()
                }
                AdminOrderResponse.of(order, userName, eventName)
            }

        // 최근 이벤트 5건
        val recentEvents = eventRepository.findTopNByOrderByCreatedAtDesc(5)
            .map { event ->
                val hostName = event.hostId?.let {
                    runCatching { hostAdaptor.findById(it).profile?.name }.getOrNull()
                }
                AdminEventResponse.of(event, hostName)
            }

        return DashboardResponse(
            totalUsers = totalUsers,
            todayNewUsers = periodNewUsers,
            todayOrders = periodOrders,
            todayRevenue = periodRevenue,
            activeEvents = activeEvents,
            todayRefunds = periodRefunds,
            recentOrders = recentOrders,
            recentEvents = recentEvents,
        )
    }
}
