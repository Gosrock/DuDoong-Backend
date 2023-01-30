package band.gosrock.api.order.model.dto.response;


import band.gosrock.domain.common.vo.EventProfileVo;
import band.gosrock.domain.common.vo.RefundInfoVo;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTickets;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketsStage;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderMethod;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderBriefDto {
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

    public static OrderBriefDto of(
            Order order, Event event , IssuedTickets issuedTickets) {
        return OrderBriefDto.builder()
                .refundInfo(event.getRefundInfoVo())
                .stage(issuedTickets.getIssuedTicketsStage())
                .orderUuid(order.getUuid())
                .orderNo(order.getOrderNo())
                .orderStatus(order.getOrderStatus())
                .eventProfile(event.toEventProfileVo())
                .build();
    }
}
