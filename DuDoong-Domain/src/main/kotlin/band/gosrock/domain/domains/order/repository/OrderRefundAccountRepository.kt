package band.gosrock.domain.domains.order.repository

import band.gosrock.domain.domains.order.domain.OrderRefundAccount
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRefundAccountRepository : JpaRepository<OrderRefundAccount, Long> {
    fun findByOrderId(orderId: Long): OrderRefundAccount?

    fun findByOrderIdIn(orderIds: Collection<Long>): List<OrderRefundAccount>
}
