package band.gosrock.api.v2.order.usecase

import band.gosrock.api.v2.order.dto.request.V2RefundAccountRequest
import band.gosrock.api.v2.order.dto.response.V2MyOrderDetailResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.service.v2.V2UserOrderDomainService
import org.slf4j.LoggerFactory

/**
 * 환불 계좌 입력·수정 (#728). 판정·저장은 도메인 서비스의 `주문` 락 + 주문 행 잠금 안에서 (엔티티를 먼저 읽지 않는다 — open-in-view).
 * 응답은 처리 후 주문 상세(O-3). 로그에는 계좌 값을 남기지 않는다
 */
@UseCase
class V2PutRefundAccountUseCase(
    private val v2UserOrderDomainService: V2UserOrderDomainService,
    private val readMyOrdersUseCase: V2ReadMyOrdersUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(userId: Long, orderUuid: String, request: V2RefundAccountRequest): V2MyOrderDetailResponse {
        v2UserOrderDomainService.putRefundAccount(userId, orderUuid, request.toForm())
        log.info("[V2PutRefundAccountUseCase] 환불 계좌 입력 userId={} orderUuid={}", userId, orderUuid)
        return readMyOrdersUseCase.detail(userId, orderUuid)
    }
}
