package band.gosrock.api.order.model.dto.response

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.common.vo.RefundInfoVo
import band.gosrock.domain.common.vo.UserInfoVo
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.user.domain.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class OrderAdminTableElement(
    @Schema(description = "취소 가능 정보")
    val refundInfo: RefundInfoVo,

    @Schema(description = "유저 정보")
    val userInfoVo: UserInfoVo,

    @Schema(description = "주문 고유 uuid 대체키임")
    val orderUuid: String,

    @Schema(description = "주문 번호 R------- 형식")
    val orderNo: String,

    @Schema(description = "주문의 상태")
    val orderStatus: OrderStatus,

    @Schema(description = "주문 이름")
    val orderName: String,

    @Schema(description = "주문 생성 시간")
    @DateFormat
    val createdAt: LocalDateTime,

    @Schema(description = "철회 완료 시간")
    @DateFormat
    val withDrawAt: LocalDateTime?,

    @Schema(description = "승인 된 시간")
    @DateFormat
    val approveAt: LocalDateTime?,

    @Schema(description = "아이템 총 갯수")
    val totalQuantity: Long,

    @Schema(description = "아이템 총 갯수")
    val totalPaymentPrice: Money,
) {
    companion object {
        @JvmStatic
        fun of(order: Order, event: Event, user: User): OrderAdminTableElement =
            OrderAdminTableElement(
                refundInfo = event.toRefundInfoVoWithOrderStatus(order.orderStatus),
                orderUuid = order.uuid!!,
                orderNo = order.orderNo!!,
                orderStatus = order.orderStatus,
                userInfoVo = user.toUserInfoVo(),
                orderName = order.orderName!!,
                createdAt = order.createdAt!!,
                withDrawAt = order.withDrawAt,
                approveAt = order.approvedAt,
                totalQuantity = order.getTotalQuantity(),
                totalPaymentPrice = order.getTotalPaymentPrice(),
            )
    }
}
