package band.gosrock.api.v2.order.usecase

import band.gosrock.api.v2.order.dto.request.V2CreateOrderRequest
import band.gosrock.api.v2.order.dto.response.V2MyOrderDetailResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.service.FreeOrderService
import band.gosrock.domain.domains.order.service.v2.V2CreateOrderCommand
import band.gosrock.domain.domains.order.service.v2.V2UserOrderDomainService
import org.slf4j.LoggerFactory

/**
 * O-1 주문 생성. 요청 검증·주문 생성은 도메인 서비스의 `티켓관리` 락 트랜잭션 안에서 하고(이 유스케이스는 엔티티를 먼저 읽지 않는다 — open-in-view),
 * 무료 선착순은 이어서 v1 무료 확정(`FreeOrderService`, `주문` 락)으로 즉시 발급한다. 발급은 커밋된 주문을 다른 트랜잭션에서 읽기 때문에
 * 생성과 한 트랜잭션으로 묶을 수 없다 (v1 도 생성 → 무료 확정 두 요청). 확정이 실패하면(동시 주문으로 매진 등) 주문을 FAILED 로 바꾸고 원래 오류를 돌려준다
 * (재시도가 중복 요청·1인 제한에 걸리지 않고 새 주문으로 진행되도록)
 */
@UseCase
class V2CreateOrderUseCase(
    private val v2UserOrderDomainService: V2UserOrderDomainService,
    private val freeOrderService: FreeOrderService,
    private val readMyOrdersUseCase: V2ReadMyOrdersUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(userId: Long, request: V2CreateOrderRequest): V2MyOrderDetailResponse {
        val ticketItemId = request.ticketItemId!!
        log.info(
            "[V2CreateOrderUseCase] 주문 생성 userId={} eventId={} ticketItemId={} quantity={} method={} applyToAll={}",
            userId, request.eventId, ticketItemId, request.quantity, request.paymentMethod, request.applyToAll(),
        )
        val created = v2UserOrderDomainService.create(
            ticketItemId,
            V2CreateOrderCommand(
                userId = userId,
                eventId = request.eventId!!,
                ticketItemId = ticketItemId,
                quantity = request.quantity!!,
                applyToAll = request.applyToAll(),
                answerSets = request.answerSets(),
                paymentChannel = request.paymentMethod!!,
                depositorName = request.depositorName,
            ),
        )
        if (created.duplicated) log.info("[V2CreateOrderUseCase] 중복 요청 → 기존 주문 반환 userId={} orderUuid={}", userId, created.orderUuid)
        if (created.needsFreeConfirm) {
            try {
                freeOrderService.execute(created.orderUuid, userId)
            } catch (e: Exception) {
                log.warn("[V2CreateOrderUseCase] 무료 확정 실패 → FAILED userId={} orderUuid={} cause={}", userId, created.orderUuid, e.message)
                runCatching { v2UserOrderDomainService.failUnconfirmed(created.orderUuid, "v2 무료 확정 실패: ${e.message}") }
                    .onFailure { log.error("[V2CreateOrderUseCase] FAILED 처리 실패 orderUuid={}", created.orderUuid, it) }
                throw e
            }
        }
        return readMyOrdersUseCase.detail(userId, created.orderUuid)
    }
}
