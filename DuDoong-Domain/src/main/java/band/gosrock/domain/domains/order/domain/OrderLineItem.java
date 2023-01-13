package band.gosrock.domain.domains.order.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.common.vo.RefundInfoVo;
import band.gosrock.domain.domains.cart.domain.CartLineItem;
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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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

    // 상품 이름
    private String productName;

    // 상품 아이디
    @JoinColumn(name = "ticket_item_id", updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private TicketItem ticketItem;
    // 상품 수량
    private Long quantity;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_line_item_id")
    private List<OrderOptionAnswer> orderOptionAnswer = new ArrayList<>();

    @Builder
    public OrderLineItem(
            TicketItem ticketItem, Long quantity, List<OrderOptionAnswer> orderOptionAnswer) {
        this.productName = ticketItem.getName();
        this.ticketItem = ticketItem;
        this.quantity = quantity;
        this.orderOptionAnswer.addAll(orderOptionAnswer);
    }

    @Builder
    public static OrderLineItem from(CartLineItem cartLineItem) {
        List<OrderOptionAnswer> orderOptionAnswers =
                cartLineItem.getCartOptionAnswers().stream().map(OrderOptionAnswer::from).toList();
        return OrderLineItem.builder()
                .orderOptionAnswer(orderOptionAnswers)
                .quantity(cartLineItem.getQuantity())
                .ticketItem(cartLineItem.getTicketItem())
                .build();
    }

    protected Money getTotalOptionAnswersPrice() {
        return orderOptionAnswer.stream()
                .map(OrderOptionAnswer::getOptionPrice)
                .reduce(Money.ZERO, Money::plus);
    }

    public Money getTotalOrderLinePrice() {
        return getItemPrice().plus(getTotalOptionAnswersPrice()).times(quantity);
    }

    public Money getItemPrice() {
        return ticketItem.getPrice();
    }

    public RefundInfoVo getRefundInfo() {
        return ticketItem.getRefundInfoVo();
    }

    public List<OptionAnswerVo> getOptionAnswerVos() {
        return orderOptionAnswer.stream().map(OrderOptionAnswer::getOptionAnswerVo).toList();
    }

    public Boolean isNeedPayment() {
        return ticketItem.isNeedPayment();
    }
}
