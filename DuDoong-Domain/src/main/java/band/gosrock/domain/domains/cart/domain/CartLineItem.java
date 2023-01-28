package band.gosrock.domain.domains.cart.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
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
@Entity(name = "tbl_cart_line_item")
public class CartLineItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_line_id")
    private Long id;

    @Column(nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private Money itemPrice;

    // 상품 수량
    @Column(nullable = false)
    private Long quantity;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_line_id")
    private List<CartOptionAnswer> cartOptionAnswers = new ArrayList<>();

    /** ---------------------------- 생성 관련 메서드 ---------------------------------- */
    @Builder
    public CartLineItem(TicketItem item, Long quantity, List<CartOptionAnswer> cartOptionAnswers) {
        this.itemId = item.getId();
        this.itemPrice = item.getPrice();
        this.quantity = quantity;
        this.cartOptionAnswers.addAll(cartOptionAnswers);
    }

    /** ---------------------------- 커맨드 메서드 ---------------------------------- */
    /** ---------------------------- 검증 메서드 ---------------------------------- */
    /** ---------------------------- 조회용 메서드 ---------------------------------- */

    /** 응답한 옵션들의 총 가격을 불러옵니다. */
    public Money getTotalOptionsPrice() {
        return cartOptionAnswers.stream()
                .map(CartOptionAnswer::getAdditionalPrice)
                .reduce(Money.ZERO, Money::plus);
    }
    /** 카트라인의 총 가격을 가져옵니다. 상품 + 옵션답변의 가격 */
    public Money getTotalCartLinePrice() {
        Money totalOptionAnswerPrice = getTotalOptionsPrice();
        return getItemPrice().plus(totalOptionAnswerPrice).times(quantity);
    }
    /** 장바구니의 담긴 상품이 결제가 필요한지. 가져옵니다. */
    public Boolean isNeedPaid() {
        // 0 < totalCartLinePrice
        return Money.ZERO.isLessThan(getTotalCartLinePrice());
    }

    public List<Long> getAnswerOptionIds() {
        return this.cartOptionAnswers.stream().map(CartOptionAnswer::getOptionId).toList();
    }
}
