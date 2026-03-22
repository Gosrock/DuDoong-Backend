package band.gosrock.domain.domains.order.repository

import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.order.repository.condition.FindEventOrdersCondition
import band.gosrock.domain.domains.order.repository.condition.FindMyPageOrderCondition
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface OrderCustomRepository {
    fun findByOrderUuid(orderUuid: String): Optional<Order>
    fun findMyOrders(condition: FindMyPageOrderCondition, pageable: Pageable): Slice<Order>
    fun findEventOrders(condition: FindEventOrdersCondition, pageable: Pageable): Page<Order>
    fun findRecentOrder(userId: Long): Optional<Order>
    fun findRefunds(eventId: Long?, refundStatus: RefundStatus?, keyword: String?, pageable: Pageable): Page<Order>
}
