package band.gosrock.domain.domains.order.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.order.exception.OrderNotFoundException
import band.gosrock.domain.domains.order.repository.OrderRepository
import band.gosrock.domain.domains.order.repository.condition.FindEventOrdersCondition
import band.gosrock.domain.domains.order.repository.condition.FindMyPageOrderCondition
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

@Adaptor
class OrderAdaptor(private val orderRepository: OrderRepository) {

    fun save(order: Order): Order = orderRepository.save(order)

    fun findById(orderId: Long): Order =
        orderRepository.findById(orderId).orElseThrow { OrderNotFoundException.EXCEPTION }

    fun findByEventId(eventId: Long): List<Order> =
        orderRepository.findByEventId(eventId)

    fun findByOrderUuid(uuid: String): Order =
        orderRepository.findByOrderUuid(uuid).orElseThrow { OrderNotFoundException.EXCEPTION }

    fun findByUuidIn(orderUuids: List<String>): List<Order> =
        orderRepository.findByUuidIn(orderUuids)

    fun findRecentOrderByUserId(userId: Long): Optional<Order> =
        orderRepository.findRecentOrder(userId)

    fun findMyOrders(condition: FindMyPageOrderCondition, pageable: Pageable): Slice<Order> =
        orderRepository.findMyOrders(condition, pageable)

    fun findEventOrders(condition: FindEventOrdersCondition, pageable: Pageable): Page<Order> =
        orderRepository.findEventOrders(condition, pageable)

    fun findByEventIdAndOrderStatus(eventId: Long, orderStatus: OrderStatus): List<Order> =
        orderRepository.findByEventIdAndOrderStatus(eventId, orderStatus)

    fun findByEventIdAndOrderStatusAndUserId(eventId: Long, userId: Long, orderStatus: OrderStatus): List<Order> =
        orderRepository.findByEventIdAndUserIdAndOrderStatus(eventId, userId, orderStatus)

    fun findRefunds(eventId: Long?, refundStatus: RefundStatus?, keyword: String?, pageable: Pageable): Page<Order> =
        orderRepository.findRefunds(eventId, refundStatus, keyword, pageable)
}
