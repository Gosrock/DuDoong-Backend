package band.gosrock.api.order.model.dto.response;


import band.gosrock.domain.common.vo.EventProfileVo;
import band.gosrock.domain.common.vo.RefundInfoVo;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderResponse {

    // 총 결제 정보
    @Schema(description = "결제 정보")
    private final OrderPaymentResponse paymentInfo;

    @Schema(description = "예매 정보( 티켓 목록 )")
    private final List<OrderLineTicketResponse> tickets;

    // 예매 취소 정보
    @Schema(description = "예매 취소 정보")
    private final RefundInfoVo refundInfo;

    @Schema(description = "이벤트 프로필 정보")
    private final EventProfileVo eventProfile;

    @Schema(description = "주문 고유 uuid")
    private final String orderUuid;

    @Schema(description = "주문 번호 R------- 형식")
    private final String orderNo;

    @Schema(description = "주문 방식 ( 결제 방식 , 승인 방식 )")
    private final OrderMethod orderMethod;

    public static OrderResponse of(
            Order order, Event event, List<OrderLineTicketResponse> tickets) {
        return OrderResponse.builder()
                .refundInfo(event.getRefundInfoVo())
                .orderMethod(order.getOrderMethod())
                .paymentInfo(OrderPaymentResponse.from(order))
                .tickets(tickets)
                .orderUuid(order.getUuid())
                .orderNo(order.getOrderNo())
                .eventProfile(event.toEventProfileVo())
                .build();
    }
}
