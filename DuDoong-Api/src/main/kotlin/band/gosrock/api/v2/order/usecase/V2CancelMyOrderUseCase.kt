package band.gosrock.api.v2.order.usecase

import band.gosrock.api.v2.order.dto.request.V2CancelMyOrderRequest
import band.gosrock.api.v2.order.dto.response.V2MyOrderDetailResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.service.v2.V2UserOrderDomainService
import org.slf4j.LoggerFactory

/** O-4 사용자 취소·환불 요청. 판정·전이는 도메인 서비스의 `주문` 락 안에서 (엔티티를 먼저 읽지 않는다 — open-in-view). 응답은 처리 후 주문 상세 */
@UseCase
class V2CancelMyOrderUseCase(
    private val v2UserOrderDomainService: V2UserOrderDomainService,
    private val readMyOrdersUseCase: V2ReadMyOrdersUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(userId: Long, orderUuid: String, request: V2CancelMyOrderRequest?): V2MyOrderDetailResponse {
        log.info("[V2CancelMyOrderUseCase] 사용자 취소 userId={} orderUuid={} refundAccount={}", userId, orderUuid, request?.refundAccount != null)
        v2UserOrderDomainService.cancel(userId, orderUuid, request?.refundAccount?.toForm())
        return readMyOrdersUseCase.detail(userId, orderUuid)
    }
}
