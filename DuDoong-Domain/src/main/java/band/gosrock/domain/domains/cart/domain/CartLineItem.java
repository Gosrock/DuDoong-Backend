package band.gosrock.domain.domains.cart.domain;


import band.gosrock.domain.domains.order.domain.PaymentInfo;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class CartLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_line_id")
    private Long id;

    @Embedded private PaymentInfo paymentInfo;
    // 상품 이름
    private String productName;
    // 상품 아이디
    private Long itemId;

    // 상품 수량
    private Long quantity;

    // 장바구니 담은 유저아이디
    private Long userId;
}
