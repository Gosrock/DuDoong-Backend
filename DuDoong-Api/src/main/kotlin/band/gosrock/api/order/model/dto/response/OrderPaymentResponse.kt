package band.gosrock.api.order.model.dto.response

import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderStatus
import io.swagger.v3.oas.annotations.media.Schema

data class OrderPaymentResponse(
    @Schema(description = "결제 수단 ( 승인 결제 , 간편 결제 , 카드 결제 등 )", defaultValue = "간편결제")
    val paymentMethod: String?,

    @Schema(
        description = "공급자 ( 카카오페이 , 현대카드 등 승인결제일경우 null 입니다. )",
        defaultValue = "카카오페이",
        nullable = true,
    )
    val provider: String?,

    @Schema(description = "공급가액 (금액)", defaultValue = "12000원")
    val supplyAmount: Money,

    @Schema(description = "할인 금액", defaultValue = "1000원")
    val discountAmount: Money,

    @Schema(description = "할인쿠폰 이름", defaultValue = "사용하지 않음")
    val couponName: String?,

    @Schema(description = "총 결제 금액", defaultValue = "11000원")
    val totalAmount: Money,

    @Schema(description = "결제 상태", defaultValue = "결제 완료")
    val orderStatus: OrderStatus,

    @Schema(description = "영수증 주소", defaultValue = "영수증주소", nullable = true)
    val receiptUrl: String?,
) {
    companion object {
        @JvmStatic
        fun from(order: Order): OrderPaymentResponse {
            val totalPaymentInfo = order.totalPaymentInfo
            return OrderPaymentResponse(
                paymentMethod = order.getMethod(),
                provider = order.getProvider(),
                discountAmount = totalPaymentInfo!!.discountAmount!!,
                supplyAmount = totalPaymentInfo.supplyAmount!!,
                totalAmount = totalPaymentInfo.paymentAmount!!,
                couponName = order.getCouponName(),
                orderStatus = order.orderStatus,
                receiptUrl = order.getReceiptUrl(),
            )
        }
    }
}
