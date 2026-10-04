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

    /** 승인 대기(PENDING_APPROVE) 주문이 있는 티켓 id */
    fun findItemIdsHavingPendingApproveOrder(itemIds: Collection<Long>): Set<Long> =
        if (itemIds.isEmpty()) emptySet()
        else orderRepository.findItemIdsHavingOrderStatus(itemIds, OrderStatus.PENDING_APPROVE).toSet()

    /** 티켓 하나의 승인 대기(PENDING_APPROVE) 주문 수량 합 */
    fun sumPendingApproveQuantity(itemId: Long): Long =
        orderRepository.sumQuantityByItemIdAndOrderStatus(itemId, OrderStatus.PENDING_APPROVE)

    /** 티켓별 승인 대기(PENDING_APPROVE) 주문 수량 합. 승인 대기 주문이 없는 티켓은 결과에 없다 */
    fun sumPendingApproveQuantities(itemIds: Collection<Long>): Map<Long, Long> =
        if (itemIds.isEmpty()) emptyMap()
        else orderRepository.sumQuantityGroupByItemIdAndOrderStatus(itemIds.toSet(), OrderStatus.PENDING_APPROVE)
            .associate { (it[0] as Number).toLong() to ((it[1] as Number?)?.toLong() ?: 0L) }

    fun findByUuidIn(orderUuids: List<String>): List<Order> =
        orderRepository.findByUuidIn(orderUuids)

    fun findRecentOrderByUserId(userId: Long): Optional<Order> =
        orderRepository.findRecentOrder(userId)

    fun findMyOrders(condition: FindMyPageOrderCondition, pageable: Pageable): Slice<Order> =
        orderRepository.findMyOrders(condition, pageable)

    fun findEventOrders(condition: FindEventOrdersCondition, pageable: Pageable): Page<Order> =
        orderRepository.findEventOrders(condition, pageable)

    fun findByEventIdAndOrderStatusAndUserId(eventId: Long, userId: Long, orderStatus: OrderStatus): List<Order> =
        orderRepository.findByEventIdAndUserIdAndOrderStatus(eventId, userId, orderStatus)

    fun findRefunds(eventId: Long?, refundStatus: RefundStatus?, keyword: String?, pageable: Pageable): Page<Order> =
        orderRepository.findRefunds(eventId, refundStatus, keyword, pageable)
}
