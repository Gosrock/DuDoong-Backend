package band.gosrock.api.order.model.dto.response;


import band.gosrock.domain.common.vo.EventProfileVo;
import band.gosrock.domain.common.vo.RefundInfoVo;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTickets;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketsStage;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderBriefElement {
    @Schema(description = "예매 취소 정보")
    private final RefundInfoVo refundInfo;

    @Schema(description = "이벤트 프로필 정보")
    private final EventProfileVo eventProfile;

    @Schema(description = "주문 고유 uuid")
    private final String orderUuid;

    @Schema(description = "주문 번호 R------- 형식")
    private final String orderNo;

    @Schema(description = "주문에 딸린 티켓들의 상태")
    private final IssuedTicketsStage stage;

    @Schema(description = "주문의 상태")
    private final OrderStatus orderStatus;

    @Schema(description = "아이템 이름")
    private final String itemName;

    @Schema(description = "아이템 총 갯수")
    private final int totalQuantity;

    public static OrderBriefElement of(Order order, Event event, IssuedTickets issuedTickets) {
        return OrderBriefElement.builder()
                .refundInfo(event.toRefundInfoVoWithOrderStatus(order.getOrderStatus()))
                .stage(issuedTickets.getIssuedTicketsStage())
                .orderUuid(order.getUuid())
                .orderNo(order.getOrderNo())
                .orderStatus(order.getOrderStatus())
                .eventProfile(event.toEventProfileVo())
                .itemName(order.getOrderName())
                .totalQuantity(issuedTickets.getTotalQuantity())
                .build();
    }
}
