package band.gosrock.domain.domains.order.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_order_line")
public class OrderLineItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_line_item_id")
    private Long id;
    // 결제 정보 ( 할인 ,공급가액, 최종 합 금액 )
    @Embedded private PaymentInfo paymentInfo;
    // 상품 이름
    private String productName;

    // 주문한 유저 아이디
    private Long userId;

    // 쿠폰 아이디 ( nullable함)
    private Long couponId;
    // 상품 아이디
    private Long itemId;
    // 상품 수량
    private Long quantity;

    @Builder
    public OrderLineItem(
            PaymentInfo paymentInfo,
            String productName,
            Long userId,
        Long couponId,
        Long itemId,
            Long quantity) {
        this.paymentInfo = paymentInfo;
        this.productName = productName;
        this.userId = userId;
        this.couponId = couponId;
        this.itemId = itemId;
        this.quantity = quantity;
    }
    // TODO : 티켓아이템에서 티켓 오더 라인 가지기 오더쪽에선 티켓을 몰라도됨?
    // TODO : 할인 쿠폰 정책 적용등?

}
