package band.gosrock.domain.domains.order.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.common.vo.RefundInfoVo;
import band.gosrock.domain.domains.cart.domain.CartLineItem;
import band.gosrock.domain.domains.event.domain.Event;
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
    /** ---------------------------- 생성 관련 메서드 ---------------------------------- */
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

    /** ---------------------------- 커맨드 메서드 ---------------------------------- */

    /** ---------------------------- 검증 메서드 ---------------------------------- */

    /** ---------------------------- 조회용 메서드 ---------------------------------- */

    /** 응답한 옵션들의 총 가격을 불러옵니다. */
    protected Money getTotalOptionAnswersPrice() {
        return orderOptionAnswer.stream()
                .map(OrderOptionAnswer::getOptionPrice)
                .reduce(Money.ZERO, Money::plus);
    }

    /** 카트라인의 총 가격을 가져옵니다. 상품 + 옵션답변의 가격 */
    public Money getTotalOrderLinePrice() {
        return getItemPrice().plus(getTotalOptionAnswersPrice()).times(quantity);
    }
    /** 상품의 가격을 가져옵니다. */
    public Money getItemPrice() {
        return ticketItem.getPrice();
    }
    /** 환불 가능 정보를 불러옵니다. */
    public RefundInfoVo getRefundInfo() {
        return ticketItem.getRefundInfoVo();
    }
    /** 옵션응답의 정보 VO를 가져옵니다. */
    public List<OptionAnswerVo> getOptionAnswerVos() {
        return orderOptionAnswer.stream().map(OrderOptionAnswer::getOptionAnswerVo).toList();
    }
    /** 결제가 필요한 오더라인인지 가져옵니다. */
    public Boolean isNeedPaid() {
        Money totalOrderLinePrice = getTotalOrderLinePrice();
        // 0 < totalOrderLinePrice
        return Money.ZERO.isLessThan(totalOrderLinePrice);
    }
    /** 주문 철회 가능 여부를 반환합니다. */
    public Boolean canRefund() {
        return this.getRefundInfo().getAvailAble();
    }
    /** 아이템의 이벤트 정보를 불러옵니다. */
    public Event getItemEvent() {
        return this.ticketItem.getEvent();
    }
}
