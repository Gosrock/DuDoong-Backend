package band.gosrock.domain.domains.order.service.v2

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderRefuseReasonType
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.order.domain.validator.OrderValidator
import band.gosrock.domain.domains.order.exception.InvalidRefuseReasonException
import band.gosrock.domain.domains.order.exception.OrderNotFoundException
import band.gosrock.domain.domains.order.exception.OrderRefundNotRequestedException
import band.gosrock.domain.domains.order.service.OrderApproveService
import band.gosrock.domain.domains.order.service.WithdrawOrderService
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * v2 호스트 주문 처리 규칙 (DEC-018, #712). v1 코드는 이 서비스를 호출하지 않는다.
 *
 * 승인·취소는 v1 도메인 서비스([OrderApproveService], [WithdrawOrderService])를 그대로 쓴다 (같은 `주문:{uuid}` 락, 같은 검증·도메인 이벤트).
 * v2 에만 있는 것: 주문이 경로의 공연 것인지 확인(다른 공연 주문은 404), 거절 사유 종류, 환불 완료 상태 검증.
 */
@DomainService
@Transactional(readOnly = true)
class V2OrderDomainService(
    private val orderAdaptor: OrderAdaptor,
    private val orderValidator: OrderValidator,
    private val orderApproveService: OrderApproveService,
    private val withdrawOrderService: WithdrawOrderService,
    private val v2OrderQuery: V2OrderQuery,
) {

    /** 이 공연의 주문. 다른 공연 주문 uuid 는 존재를 드러내지 않도록 404 */
    fun queryEventOrder(eventId: Long, orderUuid: String): Order {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        if (order.eventId != eventId) throw OrderNotFoundException.EXCEPTION
        return order
    }

    /**
     * 쓰기 전 공연 소속 확인. 엔티티를 읽지 않는다: open-in-view(test·staging·prod 기본 켜짐)라 요청 영속성 컨텍스트에 주문이 먼저 올라가면
     * v1 서비스가 락 트랜잭션(REQUIRES_NEW)에서 바꾼 상태를 같은 요청의 응답 조회가 못 본다
     */
    fun validateEventOrder(eventId: Long, orderUuid: String) {
        if (v2OrderQuery.findEventId(orderUuid) != eventId) throw OrderNotFoundException.EXCEPTION
    }

    /**
     * 승인 = v1 [OrderApproveService](`주문` 락, 새 트랜잭션). 트랜잭션 밖(NOT_SUPPORTED)에서 부른다 (#746): 클래스의 읽기 전용 트랜잭션이 열려 있으면
     * 그 커넥션을 쥔 채 v1 락을 기다린다 (#743 후속).
     * - NOT_SUPPORTED 범위에서는 **자체 트랜잭션을 가진 조회만** 쓴다 (소속 확인 [V2OrderQuery.findEventId]) — 그냥 읽으면 범위 끝까지 커넥션을 쥔다
     * - 효과 조건: **호출 측도 트랜잭션 없이** 불러야 하고(있으면 그 커넥션을 쥔 채 기다린다), **open-in-view 가 꺼져 있어야** 한다
     *   (켜져 있으면 요청 EntityManager 가 락 전 조회의 커넥션을 쥐어 절감이 없다 — test·staging·prod 기본값은 켜짐)
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun approve(eventId: Long, orderUuid: String) {
        validateEventOrder(eventId, orderUuid)
        orderApproveService.execute(orderUuid)
    }

    /** 승인 완료(APPROVED/CONFIRM) 주문 취소 (v1 과 같은 로직). [approve] 와 같은 이유·조건으로 트랜잭션 밖에서 v1 [WithdrawOrderService] 락을 기다린다 (#746) */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun cancel(eventId: Long, orderUuid: String, reason: String?) {
        validateEventOrder(eventId, orderUuid)
        withdrawOrderService.cancelOrder(orderUuid, reason?.trim()?.ifEmpty { null })
    }

    /**
     * 승인 대기 주문 거절. 상태는 v1 과 같은 CANCELED(+ 환불 요청)이고, 사유 종류는 refuse_reason_type,
     * 표시 문구는 v1 화면 호환을 위해 cancel_reason 에 기록한다 (기타는 직접 입력값)
     */
    @RedissonLock(LockName = ORDER_LOCK, identifier = "orderUuid")
    fun refuse(eventId: Long, orderUuid: String, reasonType: OrderRefuseReasonType, reasonText: String?) {
        val reason = refuseReasonText(reasonType, reasonText)
        val order = queryEventOrder(eventId, orderUuid)
        order.refuse(orderValidator, reason)
        order.recordRefuseReasonType(reasonType)
    }

    /** 환불 완료 (DEC-016). 환불 요청 상태만 가능, 이미 완료면 그대로 성공(멱등) */
    @RedissonLock(LockName = ORDER_LOCK, identifier = "orderUuid")
    fun completeRefund(eventId: Long, orderUuid: String) {
        val order = queryEventOrder(eventId, orderUuid)
        when (order.refundStatus) {
            RefundStatus.REFUND_COMPLETED -> return
            RefundStatus.NONE -> throw OrderRefundNotRequestedException.EXCEPTION
            RefundStatus.REFUND_REQUESTED -> order.completeRefund()
        }
    }

    /** 거절 표시 문구. 기타(ETC)는 직접 입력 1~20자(앞뒤 공백 제외) 필수, 그 외는 입력값을 무시하고 종류 문구 */
    fun refuseReasonText(reasonType: OrderRefuseReasonType, reasonText: String?): String {
        if (reasonType != OrderRefuseReasonType.ETC) return reasonType.label
        val text = reasonText?.trim().orEmpty()
        if (text.isEmpty() || text.length > REFUSE_REASON_MAX_LENGTH) throw InvalidRefuseReasonException.EXCEPTION
        return text
    }

    companion object {
        private const val ORDER_LOCK = "주문"
        const val REFUSE_REASON_MAX_LENGTH = 20
    }
}
