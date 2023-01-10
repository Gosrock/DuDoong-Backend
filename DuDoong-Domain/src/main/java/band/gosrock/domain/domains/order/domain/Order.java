package band.gosrock.domain.domains.order.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import band.gosrock.domain.domains.order.exception.InvalidOrderException;
import band.gosrock.domain.domains.order.exception.NotMyOrderException;
import band.gosrock.domain.domains.order.exception.NotPendingOrderException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_order")
public class Order extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    // 주문한 유저아이디
    private Long userId;

    // 토스페이먼츠용 주문번호
    @Column(nullable = false)
    private String uuid;

    private String orderName;

    // 결제 방식 ( 토스 승인 이후 저장 )
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    // 토스 결제 승인후 결제 긁힌 시간
    private LocalDateTime approvedAt;

    // 세금
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "vat_amount"))
    private Money vat;

    // 결제 정보
    @Embedded private PaymentInfo totalPaymentInfo;

    // 주문 상태
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    // 발급된 쿠폰 정보
    @JoinColumn(name = "issued_coupon_id", updatable = false)
    @OneToOne(fetch = FetchType.LAZY)
    private IssuedCoupon issuedCoupon;

    // 단방향 oneToMany 매핑
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private List<OrderLineItem> orderLineItems = new ArrayList<>();

    @PrePersist
    public void addUUID() {
        this.uuid = UUID.randomUUID().toString();
    }

    @Builder
    public Order(Long userId, String OrderName, List<OrderLineItem> orderLineItems) {
        this.userId = userId;
        this.orderName = OrderName;
        this.orderLineItems.addAll(orderLineItems);
    }

    public static Order createOrder(Long userId, Cart cart) {
        List<OrderLineItem> orderLineItems =
                cart.getCartLineItems().stream().map(OrderLineItem::from).toList();
        return Order.builder()
                .userId(userId)
                .OrderName(cart.getCartName())
                .orderLineItems(orderLineItems)
                .build();
    }

    public Money getTotalSupplyPrice() {
        return orderLineItems.stream()
                .map(OrderLineItem::getTotalOrderLinePrice)
                .reduce(Money.ZERO, Money::plus);
    }

    public Money getTotalPaymentPrice() {
        return getTotalSupplyPrice().minus(getTotalDiscountPrice());
    }

    public Money getTotalDiscountPrice() {
        if (issuedCoupon != null) {
            return issuedCoupon.getDiscountAmount(getTotalSupplyPrice());
        }
        return Money.ZERO;
    }

    public void calculatePaymentInfo() {
        totalPaymentInfo =
                PaymentInfo.builder()
                        .discountAmount(getTotalDiscountPrice())
                        .paymentAmount(getTotalPaymentPrice())
                        .supplyAmount(getTotalSupplyPrice())
                        .build();
    }


    public void confirmOrder(Long currentUserId , Money requestAmount){
        if(!userId.equals(currentUserId)){
            throw NotMyOrderException.EXCEPTION;
        }
        if(!getTotalPaymentPrice().equals(requestAmount)){
            throw InvalidOrderException.EXCEPTION;
        }
        if(!orderStatus.equals(OrderStatus.PENDING)){
            throw NotPendingOrderException.EXCEPTION;
        }
        //TODO: 재고량 비교 필요?
        orderStatus = OrderStatus.CONFIRM;
    }
    public void updatePaymentInfo(LocalDateTime paymentAt,PaymentMethod paymentMethod,Money vat){

    }
}
