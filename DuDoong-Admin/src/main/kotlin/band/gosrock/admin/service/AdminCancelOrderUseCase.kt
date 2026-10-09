package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminOrderResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.service.WithdrawOrderService
import band.gosrock.domain.domains.user.adaptor.UserAdaptor

@UseCase
class AdminCancelOrderUseCase(
    private val orderAdaptor: OrderAdaptor,
    private val withdrawOrderService: WithdrawOrderService,
    private val userAdaptor: UserAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val adminAuthValidator: AdminAuthValidator,
) {

    /**
     * 운영 취소. v1 호스트 취소와 같은 락·검증을 쓰는 [WithdrawOrderService.cancelOrderByAdmin] (`주문:{uuid}` 락 + 새 트랜잭션, 공연 경로 없음 #760)로 처리한다 (#719):
     * 선물 수락·생성과 같은 주문 락으로 줄 서고, 선물 연쇄 처리(대기 선물 무효)도 같은 트랜잭션에서 된다.
     * 응답은 커밋 뒤에 다시 읽으므로 바깥 트랜잭션을 두지 않는다 (REPEATABLE READ 스냅샷에 옛 상태가 남지 않게)
     */
    fun execute(userId: Long, orderUuid: String, reason: String? = null): AdminOrderResponse {
        adminAuthValidator.validateAdminOrAbove(userId)
        withdrawOrderService.cancelOrderByAdmin(orderUuid, reason)
        val order = orderAdaptor.findByOrderUuid(orderUuid)

        val userName = order.userId?.let {
            runCatching { userAdaptor.queryUser(it).profile?.name }.getOrNull()
        }
        val eventName = order.eventId?.let {
            runCatching { eventAdaptor.findById(it).eventBasic?.name }.getOrNull()
        }
        return AdminOrderResponse.of(order, userName, eventName)
    }
}
