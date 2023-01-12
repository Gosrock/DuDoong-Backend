package band.gosrock.domain.domains.cart.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.domain.TicketType;
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
import javax.persistence.ManyToOne;
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

    // 상품
    @JoinColumn(name = "ticket_item_id", updatable = false, nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private TicketItem ticketItem;

    // 상품 수량
    @Column(nullable = false)
    private Long quantity;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_line_id")
    private List<CartOptionAnswer> cartOptionAnswers = new ArrayList<>();

    @Builder
    public CartLineItem(
            TicketItem ticketItem, Long quantity, List<CartOptionAnswer> cartOptionAnswers) {
        this.ticketItem = ticketItem;
        this.quantity = quantity;
        this.cartOptionAnswers.addAll(cartOptionAnswers);
    }

    public Money getTotalOptionsPrice() {
        return cartOptionAnswers.stream()
                .map(CartOptionAnswer::getOptionPrice)
                .reduce(Money.ZERO, Money::plus);
    }

    public String getTicketName() {
        return ticketItem.getName();
    }

    public Money getTicketPrice() {
        return ticketItem.getPrice();
    }

    public Money getTotalCartLinePrice() {
        Money totalOptionAnswerPrice = getTotalOptionsPrice();
        return ticketItem.getPrice().plus(totalOptionAnswerPrice).times(quantity);
    }

    public TicketType getTicketType() {
        return ticketItem.getType();
    }

    public List<OptionAnswerVo> getOptionAnswerVos() {
        return cartOptionAnswers.stream().map(CartOptionAnswer::getOptionAnswerVo).toList();
    }

    public Money getItemPrice() {
        return ticketItem.getPrice();
    }
}
