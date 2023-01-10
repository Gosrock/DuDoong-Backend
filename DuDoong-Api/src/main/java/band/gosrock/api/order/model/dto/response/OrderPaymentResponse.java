package band.gosrock.api.order.model.dto.response;


import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.PaymentInfo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderPaymentResponse {
    private final String paymentMethod;
    private final String supplyAmount;
    private final String discountAmount;
    private final String couponName;
    private final String totalAmount;
    private final String orderStatus;

    public static OrderPaymentResponse from(Order order) {
        PaymentInfo totalPaymentInfo = order.getTotalPaymentInfo();

        return OrderPaymentResponse.builder()
                .paymentMethod(order.getPaymentMethod().toString())
                .discountAmount(totalPaymentInfo.getDiscountAmount().toString())
                .supplyAmount(totalPaymentInfo.getSupplyAmount().toString())
                .totalAmount(totalPaymentInfo.getPaymentAmount().toString())
                .couponName(order.getCouponName())
                .orderStatus(order.getOrderStatus().toString())
                .build();
    }
}
