package band.gosrock.api.order.service

import band.gosrock.api.order.model.dto.request.RefundRequest
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class RequestRefundUseCase(
    private val orderAdaptor: OrderAdaptor,
) {

    @Transactional
    fun execute(userId: Long, orderUuid: String, request: RefundRequest) {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        require(order.userId == userId) { "본인의 주문만 환불 요청할 수 있습니다." }
        order.requestRefund(request.reason)
    }
}
