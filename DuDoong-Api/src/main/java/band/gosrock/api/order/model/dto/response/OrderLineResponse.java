package band.gosrock.api.order.model.dto.response;

import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.common.vo.RefundInfoVo;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderLineResponse {

    // 결제 정보
    private final OrderLinePaymentResponse orderLinePaymentResponse;
    // 예매 취소 정보
    private final RefundInfoVo refundInfoVo;
    // 예매 정보
    private final OrderLineTicketResponse orderLineTicketResponse;
    // 응답 정보
    private List<OptionAnswerVo> answers;
}
