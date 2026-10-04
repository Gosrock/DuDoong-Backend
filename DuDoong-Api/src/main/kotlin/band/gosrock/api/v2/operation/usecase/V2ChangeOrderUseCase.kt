package band.gosrock.api.v2.operation.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.operation.dto.request.V2CancelOrderRequest
import band.gosrock.api.v2.operation.dto.request.V2RefuseOrderRequest
import band.gosrock.api.v2.operation.dto.response.V2OrderDetailResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.service.v2.V2OrderDomainService
import org.slf4j.LoggerFactory

/** R-3 승인 / R-4 거절 / R-5 취소 / F-2 환불 완료 (매니저 이상, DEC-009). 응답은 처리 후 주문 상세 */
@UseCase
class V2ChangeOrderUseCase(
    private val v2OrderDomainService: V2OrderDomainService,
    private val readOrdersUseCase: V2ReadOrdersUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun approve(userId: Long, eventId: Long, orderUuid: String): V2OrderDetailResponse {
        log.info("[V2ChangeOrderUseCase] 승인 userId={} eventId={} orderUuid={}", userId, eventId, orderUuid)
        v2OrderDomainService.approve(eventId, orderUuid)
        return readOrdersUseCase.readDetail(eventId, orderUuid, showRefundAccount = true)
    }

    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun refuse(userId: Long, eventId: Long, orderUuid: String, request: V2RefuseOrderRequest): V2OrderDetailResponse {
        log.info("[V2ChangeOrderUseCase] 거절 userId={} eventId={} orderUuid={} reasonType={}", userId, eventId, orderUuid, request.reasonType)
        v2OrderDomainService.refuse(eventId, orderUuid, request.reasonType!!, request.reasonText)
        return readOrdersUseCase.readDetail(eventId, orderUuid, showRefundAccount = true)
    }

    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun cancel(userId: Long, eventId: Long, orderUuid: String, request: V2CancelOrderRequest?): V2OrderDetailResponse {
        log.info("[V2ChangeOrderUseCase] 취소 userId={} eventId={} orderUuid={}", userId, eventId, orderUuid)
        v2OrderDomainService.cancel(eventId, orderUuid, request?.reason)
        return readOrdersUseCase.readDetail(eventId, orderUuid, showRefundAccount = true)
    }

    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun completeRefund(userId: Long, eventId: Long, orderUuid: String): V2OrderDetailResponse {
        log.info("[V2ChangeOrderUseCase] 환불 완료 userId={} eventId={} orderUuid={}", userId, eventId, orderUuid)
        v2OrderDomainService.completeRefund(eventId, orderUuid)
        return readOrdersUseCase.readDetail(eventId, orderUuid, showRefundAccount = true)
    }
}
