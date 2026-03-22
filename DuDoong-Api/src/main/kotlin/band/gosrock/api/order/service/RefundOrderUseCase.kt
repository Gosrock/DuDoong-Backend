package band.gosrock.api.order.service

import band.gosrock.api.order.model.dto.response.OrderResponse
import band.gosrock.api.order.model.mapper.OrderMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.service.WithdrawOrderService
import org.slf4j.LoggerFactory

/** 환불을 위함 환불이라 함은, 사용자가 직접 구매한 물품을 취소시킴 */
@UseCase
class RefundOrderUseCase(
    private val withdrawOrderService: WithdrawOrderService,
    private val orderMapper: OrderMapper,
) {
    private val log = LoggerFactory.getLogger(RefundOrderUseCase::class.java)

    fun execute(userId: Long, orderUuid: String, reason: String? = null): OrderResponse {
        log.info("[RefundOrderUseCase][execute] 환불 요청 userId={} orderUuid={} reason={}", userId, orderUuid, reason)
        withdrawOrderService.refundOrder(orderUuid, userId, reason)
        return orderMapper.toOrderResponse(orderUuid)
    }
}
