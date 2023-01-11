package band.gosrock.api.order.model.dto.response;


import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderLineTicketResponse {
    @Schema(description = "티켓명", defaultValue = "일반티켓")
    private final String ticketName;

    @Schema(description = "예매 번호", defaultValue = "R1000001-123")
    private final String orderNo;

    @Schema(description = "티켓 번호", defaultValue = "T1000001 ~ T1000002 (2매)")
    private final String ticketNos;

    @Schema(description = "구매 일시")
    private final LocalDateTime paymentAt;

    @Schema(description = "유저이름")
    private final String userName;

    @Schema(description = "금액")
    private final String orderLinePrice;

    @Schema(description = "구매수량")
    private final Long purchaseQuantity;

    @Schema(description = "옵션의 응답 목록")
    private List<OptionAnswerVo> answers;

    public static OrderLineTicketResponse of(
            Order order, OrderLineItem orderLineItem, String userName, String ticketNos) {
        return OrderLineTicketResponse.builder()
                .answers(orderLineItem.getOptionAnswerVos())
                .orderNo(order.getOrderNo() + "-" + orderLineItem.getId())
                .ticketNos(ticketNos)
                .ticketName(orderLineItem.getProductName())
                .paymentAt(order.getApprovedAt())
                .userName(userName)
                .orderLinePrice(orderLineItem.getTotalOrderLinePrice().toString())
                .purchaseQuantity(orderLineItem.getQuantity())
                .build();
    }
}
