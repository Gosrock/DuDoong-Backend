package band.gosrock.infrastructure.outer.api.tossPayments.dto.request;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CancelPaymentsRequest {
    private String cancelReason;
    // 부분 취소 안합니다. 전체 금액 취소입니다.
}
