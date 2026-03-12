package band.gosrock.domain.domains.order.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.PgPaymentInfo
import band.gosrock.domain.domains.order.domain.validator.OrderValidator
import band.gosrock.infrastructure.outer.api.tossPayments.client.PaymentsConfirmClient
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.ConfirmPaymentsRequest
import org.springframework.transaction.annotation.Transactional

@DomainService
@Transactional(readOnly = true)
class OrderConfirmService(
    private val paymentsConfirmClient: PaymentsConfirmClient,
    private val orderAdaptor: OrderAdaptor,
    private val orderValidator: OrderValidator,
) {
    @RedissonLock(LockName = "주문", identifier = "orderId", paramClassType = ConfirmPaymentsRequest::class)
    fun execute(confirmPaymentsRequest: ConfirmPaymentsRequest, currentUserId: Long): String {
        val order = orderAdaptor.findByOrderUuid(confirmPaymentsRequest.orderId!!)
        val paymentWons = Money.wons(confirmPaymentsRequest.amount!!)
        orderValidator.validOwner(order, currentUserId)
        orderValidator.validMethodIsPaymentOrder(order)
        orderValidator.validAmountIsSameAsRequest(order, paymentWons)
        val paymentsResponse = paymentsConfirmClient.execute(confirmPaymentsRequest)
        order.confirmPayment(
            paymentsResponse.approvedAt!!.toLocalDateTime(),
            PgPaymentInfo.from(paymentsResponse),
            orderValidator,
        )
        return order.uuid!!
    }
}
