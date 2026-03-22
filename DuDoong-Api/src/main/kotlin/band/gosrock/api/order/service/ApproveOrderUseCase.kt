package band.gosrock.api.order.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.order.model.dto.response.OrderResponse
import band.gosrock.api.order.model.mapper.OrderMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.service.OrderApproveService
import org.slf4j.LoggerFactory

@UseCase
class ApproveOrderUseCase(
    private val orderApproveService: OrderApproveService,
    private val orderMapper: OrderMapper,
) {
    private val log = LoggerFactory.getLogger(ApproveOrderUseCase::class.java)

    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(userId: Long, eventId: Long, orderUuid: String): OrderResponse {
        log.info("[ApproveOrderUseCase][execute] 주문 승인 userId={} eventId={} orderUuid={}", userId, eventId, orderUuid)
        val confirmOrderUuid = orderApproveService.execute(orderUuid)
        return orderMapper.toOrderResponse(confirmOrderUuid)
    }
}
