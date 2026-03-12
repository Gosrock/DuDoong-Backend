package band.gosrock.domain.domains.order.repository

import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderStatus
import org.springframework.data.repository.CrudRepository

interface OrderRepository : CrudRepository<Order, Long>, OrderCustomRepository {
    fun findByEventId(eventId: Long): List<Order>
    fun findByEventIdAndOrderStatus(eventId: Long, orderStatus: OrderStatus): List<Order>
    fun findByEventIdAndUserIdAndOrderStatus(eventId: Long, userId: Long, orderStatus: OrderStatus): List<Order>
    fun findByUuidIn(uuids: List<String>): List<Order>
}
