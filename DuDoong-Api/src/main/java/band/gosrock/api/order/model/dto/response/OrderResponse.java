package band.gosrock.api.order.model.dto.response;


import band.gosrock.domain.common.vo.RefundInfoVo;
import band.gosrock.domain.domains.order.domain.Order;
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

    public static OrderResponse of(Order order, List<OrderLineTicketResponse> tickets) {
        return OrderResponse.builder()
                .refundInfo(order.getTotalRefundInfo())
                .paymentInfo(OrderPaymentResponse.from(order))
                .tickets(tickets)
                .build();
    }
}
