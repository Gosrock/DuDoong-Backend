package band.gosrock.domain.domains.cart.domain;


import band.gosrock.domain.domains.item.domain.ItemId;
import band.gosrock.domain.domains.order.domain.PaymentInfo;
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
public class CartLineItem {

    @EmbeddedId private CartLineId id;

    @Embedded private PaymentInfo paymentInfo;
    // 상품 이름
    private String productName;
    // 상품 아이디
    @Embedded private ItemId itemId;

    // 상품 수량
    private Long quantity;

    // 장바구니 담은 유저아이디
    @Embedded private UserId userId;
}
