package band.gosrock.domain.domains.order.domain;


import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {
    // 최초 상태
    PENDING("PENDING", "주문 생성상태"),
    // 결제 대기중
    PENDING_PAYMENT("PENDING_PAYMENT", "결제 대기중"),
    // 승인 대기중 ( 승인 결제 시 )
    PENDING_APPROVE("PENDING_APPROVE", "승인 대기중"),
    // 만료 ( 주문 대기후 결제가 장기간 이뤄지지 않았을 때 )
    OUTDATED("OUTDATED", "결제 시간 만료"),
    // 결제 승인
    CONFIRM("CONFIRM", "결제 완료"),
    // 환불
    REFUND("REFUND", "환불 완료"),
    // 토스 결제 실패시
    FAILED("FAILED", "결제 실패");
    private String value;

    @JsonValue private String kr;
}
