package band.gosrock.domain.domains.order;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.coupon.domain.CouponId;
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
public class TicketOrderLine extends BaseTimeEntity {

    @EmbeddedId private OrderLineId id;

    // 총 결제 금액
    @Embedded private Money paymentAmount;

    // 상품 이름
    private String productName;

    // 주문한 유저 아이디
    @Embedded private UserId userId;

    // 할인 금액
    @Embedded private Money paymentDiscount;
    // 쿠폰 아이디 ( nullable함)
    @Embedded private CouponId couponId;


    // TODO : 티켓아이템에서 티켓 오더 라인 가지기
    // TODO : 할인 쿠폰 정책 적용등?

}
