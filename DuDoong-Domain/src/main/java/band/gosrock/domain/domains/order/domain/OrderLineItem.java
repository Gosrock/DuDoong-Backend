package band.gosrock.domain.domains.order.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.cart.domain.CartLineItem;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
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
@Entity(name = "tbl_order_line")
public class OrderLineItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_line_item_id")
    private Long id;

    // 상품 관련
    @Embedded private OrderItemVo orderItem;
    // 상품 수량
    private Long quantity;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_line_item_id")
    private List<OrderOptionAnswer> orderOptionAnswers = new ArrayList<>();
    /** ---------------------------- 생성 관련 메서드 ---------------------------------- */
    @Builder
    public OrderLineItem(
            TicketItem ticketItem, Long quantity, List<OrderOptionAnswer> orderOptionAnswer) {
        this.orderItem = OrderItemVo.from(ticketItem);
        this.quantity = quantity;
        this.orderOptionAnswers.addAll(orderOptionAnswer);
    }

    @Builder
    public static OrderLineItem of(CartLineItem cartLineItem, TicketItem ticketItem) {
        List<OrderOptionAnswer> orderOptionAnswers =
                cartLineItem.getCartOptionAnswers().stream().map(OrderOptionAnswer::from).toList();
        return OrderLineItem.builder()
                .orderOptionAnswer(orderOptionAnswers)
                .quantity(cartLineItem.getQuantity())
                .ticketItem(ticketItem)
                .build();
    }

    /** ---------------------------- 커맨드 메서드 ---------------------------------- */

    /** ---------------------------- 검증 메서드 ---------------------------------- */

    /** ---------------------------- 조회용 메서드 ---------------------------------- */

    /** 응답한 옵션들의 총 가격을 불러옵니다. */
    public Money getOptionAnswersPrice() {
        return orderOptionAnswers.stream()
                .map(OrderOptionAnswer::getAdditionalPrice)
                .reduce(Money.ZERO, Money::plus);
    }

    /** 카트라인의 총 가격을 가져옵니다. 상품 + 옵션답변의 가격 */
    public Money getTotalOrderLinePrice() {
        return getItemPrice().plus(getOptionAnswersPrice()).times(quantity);
    }
    /** 상품의 가격을 가져옵니다. */
    public Money getItemPrice() {
        return orderItem.getPrice();
    }
    /** 결제가 필요한 오더라인인지 가져옵니다. */
    public Boolean isNeedPaid() {
        Money totalOrderLinePrice = getTotalOrderLinePrice();
        // 0 < totalOrderLinePrice
        return Money.ZERO.isLessThan(totalOrderLinePrice);
    }

    /** 아이템 아이디를 조회합니다. */
    public Long getItemId() {
        return orderItem.getItemId();
    }

    /** 아이템 그룹 아이디를 조회합니다 ( 이벤트 아이디 ) */
    public Long getItemGroupId() {
        return orderItem.getItemGroupId();
    }

    public String getItemName() {
        return orderItem.getName();
    }

    public List<Long> getAnswerOptionIds() {
        return this.orderOptionAnswers.stream().map(OrderOptionAnswer::getOptionId).toList();
    }
}
