package band.gosrock.helper.excel;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderMethod;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExcelOrderDto {

    private final String orderNo;
    private final OrderMethod orderMethod;
    private final OrderStatus orderStatus;
    private final String orderName;
    private final Long userId;
    private final Money amount;
    private final Long quantity;
    private final LocalDateTime createdAt;
    private final LocalDateTime refundAt;

    public static ExcelOrderDto from(Order order) {
        return ExcelOrderDto.builder()
                .amount(order.getTotalPaymentPrice())
                .orderNo(order.getOrderNo())
                .orderMethod(order.getOrderMethod())
                .orderStatus(order.getOrderStatus())
                .orderName(order.getOrderName())
                .userId(order.getUserId())
                .quantity(order.getTotalQuantity())
                .createdAt(order.getCreatedAt())
                .refundAt(order.getWithDrawAt())
                .build();
    }
}
