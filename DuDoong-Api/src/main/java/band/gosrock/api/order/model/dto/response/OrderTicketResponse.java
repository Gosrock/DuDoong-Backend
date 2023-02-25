package band.gosrock.api.order.model.dto.response;


import band.gosrock.domain.common.vo.EventProfileVo;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.order.domain.Order;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderTicketResponse {

    @Schema(description = "예매 정보( 티켓 목록 )")
    private final List<IssuedTicketInfoVo> tickets;

    @Schema(description = "이벤트 프로필 정보")
    private final EventProfileVo eventProfile;

    @Schema(description = "주문 고유 uuid")
    private final String orderUuid;

    @Schema(description = "주문 번호 R------- 형식")
    private final String orderNo;

    public static OrderTicketResponse of(
            Order order, Event event, List<IssuedTicketInfoVo> tickets) {
        return OrderTicketResponse.builder()
                .tickets(tickets)
                .orderUuid(order.getUuid())
                .orderNo(order.getOrderNo())
                .eventProfile(event.toEventProfileVo())
                .build();
    }
}
