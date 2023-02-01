package band.gosrock.api.order.model.dto.response;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.RefundInfoVo;
import band.gosrock.domain.common.vo.UserInfoVo;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import band.gosrock.domain.domains.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderAdminTableElement {
    @Schema(description = "취소 가능 정보")
    private final RefundInfoVo refundInfo;

    @Schema(description = "유저 정보")
    private final UserInfoVo userInfoVo;

    @Schema(description = "주문 고유 uuid 대체키임")
    private final String orderUuid;

    @Schema(description = "주문 번호 R------- 형식")
    private final String orderNo;

    @Schema(description = "주문의 상태")
    private final OrderStatus orderStatus;

    @Schema(description = "주문 이름")
    private final String orderName;

    @Schema(description = "주문 생성 시간")
    private final LocalDateTime createdAt;

    @Schema(description = "철회 완료 시간")
    private final LocalDateTime withDrawAt;

    @Schema(description = "승인 된 시간")
    private final LocalDateTime approveAt;

    @Schema(description = "아이템 총 갯수")
    private final Long totalQuantity;

    @Schema(description = "아이템 총 갯수")
    private final Money totalPaymentPrice;

    public static OrderAdminTableElement of(Order order, Event event, User user) {
        return OrderAdminTableElement.builder()
                .refundInfo(event.toRefundInfoVo())
                .orderUuid(order.getUuid())
                .orderNo(order.getOrderNo())
                .orderStatus(order.getOrderStatus())
                .userInfoVo(user.toUserInfoVo())
                .orderName(order.getOrderName())
                .createdAt(order.getCreatedAt())
                .withDrawAt(order.getWithDrawAt())
                .approveAt(order.getApprovedAt())
                .totalQuantity(order.getTotalQuantity())
                .totalPaymentPrice(order.getTotalPaymentPrice())
                .build();
    }
}
