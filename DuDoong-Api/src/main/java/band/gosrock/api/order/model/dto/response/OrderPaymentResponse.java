package band.gosrock.api.order.model.dto.response;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import band.gosrock.domain.domains.order.domain.PaymentInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderPaymentResponse {
    @Schema(description = "결제 수단 ( 승인 결제 , 간편 결제 , 카드 결제 등 )", defaultValue = "간편결제")
    private final String paymentMethod;

    @Schema(
            description = "공급자 ( 카카오페이 , 현대카드 등 승인결제일경우 null 입니다. )",
            defaultValue = "카카오페이",
            nullable = true)
    private final String provider;

    @Schema(description = "공급가액 (금액)", defaultValue = "12000원")
    private final Money supplyAmount;

    @Schema(description = "할인 금액", defaultValue = "1000원")
    private final Money discountAmount;

    @Schema(description = "할인쿠폰 이름", defaultValue = "사용하지 않음")
    private final String couponName;

    @Schema(description = "총 결제 금액", defaultValue = "11000원")
    private final Money totalAmount;

    @Schema(description = "결제 상태", defaultValue = "결제 완료")
    private final OrderStatus orderStatus;

    @Schema(description = "영수증 주소", defaultValue = "영수증주소", nullable = true)
    private final String receiptUrl;

    public static OrderPaymentResponse from(Order order) {
        PaymentInfo totalPaymentInfo = order.getTotalPaymentInfo();

        return OrderPaymentResponse.builder()
                .paymentMethod(order.getMethod())
                .provider(order.getProvider())
                .discountAmount(totalPaymentInfo.getDiscountAmount())
                .supplyAmount(totalPaymentInfo.getSupplyAmount())
                .totalAmount(totalPaymentInfo.getPaymentAmount())
                .couponName(order.getCouponName())
                .orderStatus(order.getOrderStatus())
                .receiptUrl(order.getReceiptUrl())
                .build();
    }
}
