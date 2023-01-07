package band.gosrock.domain.domains.order.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.domains.coupon.domain.CouponId;
import band.gosrock.domain.domains.item.domain.ItemId;
import band.gosrock.domain.domains.user.domain.UserId;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class OrderLineItem extends BaseTimeEntity {

    @EmbeddedId private OrderLineItemId id;

    // 결제 정보 ( 할인 ,공급가액, 최종 합 금액 )
    @Embedded private PaymentInfo paymentInfo;
    // 상품 이름
    private String productName;

    // 주문한 유저 아이디
    @Embedded private UserId userId;

    // 쿠폰 아이디 ( nullable함)
    @Embedded private CouponId couponId;
    // 상품 아이디
    @Embedded private ItemId itemId;
    // 상품 수량
    private Long quantity;

    // TODO : 티켓아이템에서 티켓 오더 라인 가지기 오더쪽에선 티켓을 몰라도됨?
    // TODO : 할인 쿠폰 정책 적용등?

}
