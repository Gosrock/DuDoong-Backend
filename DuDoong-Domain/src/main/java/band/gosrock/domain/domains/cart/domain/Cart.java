package band.gosrock.domain.domains.cart.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.cart.exception.CartLineItemNotFoundException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_cart")
public class Cart extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private String cartName;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private List<CartLineItem> cartLineItems = new ArrayList<>();
    /** ---------------------------- 생성 관련 메서드 ---------------------------------- */
    @Builder
    public Cart(Long userId, List<CartLineItem> cartLineItems) {
        this.userId = userId;
        this.cartLineItems.addAll(cartLineItems);
    }

    public static Cart of(
            List<CartLineItem> cartLineItems,
            String itemName,
            Long userId,
            CartValidator cartValidator) {
        Cart cart = Cart.builder().cartLineItems(cartLineItems).userId(userId).build();
        cartValidator.validCanCreate(cart);
        cart.updateCartName(itemName);
        return cart;
    }

    /** ---------------------------- 커맨드 메서드 ---------------------------------- */
    private void updateCartName(String name) {
        cartName = name;
    }

    /** ---------------------------- 검증 메서드 ---------------------------------- */
    /** ---------------------------- 조회용 메서드 ---------------------------------- */
    /** 결제가 필요한 오더인지 반환합니다. */
    public Boolean isNeedPaid() {
        return this.cartLineItems.stream()
                .map(CartLineItem::isNeedPaid)
                .reduce(Boolean.FALSE, (Boolean::logicalOr));
    }

    /** 카트에 담긴 상품의 총 갯수를 가져옵니다. */
    public Long getTotalQuantity() {
        return cartLineItems.stream().map(CartLineItem::getQuantity).reduce(0L, Long::sum);
    }
    /** 카트에 담긴 상품의 총 가격을 가져옵니다 . (상품 + 옵션 ) * 총갯수 = 카트라인의 가격 , 카트라인의 가격의 합집합 */
    public Money getTotalPrice() {
        return cartLineItems.stream()
                .map(CartLineItem::getTotalCartLinePrice)
                .reduce(Money.ZERO, Money::plus);
    }

    public Long getItemId() {
        return getCartLineItem().getItemId();
    }

    private CartLineItem getCartLineItem() {
        return cartLineItems.stream()
                .findFirst()
                .orElseThrow(() -> CartLineItemNotFoundException.EXCEPTION);
    }
}
