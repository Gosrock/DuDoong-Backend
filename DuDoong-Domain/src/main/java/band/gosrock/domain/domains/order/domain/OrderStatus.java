package band.gosrock.domain.domains.order.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {
    // 결제 대기중
    PENDING("PENDING"),
    // 만료 ( 주문 대기후 결제가 장기간 이뤄지지 않았을 때 )
    OUTDATED("OUTDATED"),
    // 결제 승인
    CONFIRM("CONFIRM"),
    // 환불
    REFUND("REFUND");
    private String value;
}
