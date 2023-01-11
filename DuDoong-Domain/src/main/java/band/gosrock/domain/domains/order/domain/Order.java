package band.gosrock.domain.domains.order.domain;

import static band.gosrock.common.consts.DuDoongStatic.NO_START_NUMBER;

import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.RefundInfoVo;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import band.gosrock.domain.domains.order.exception.InvalidOrderException;
import band.gosrock.domain.domains.order.exception.NotMyOrderException;
import band.gosrock.domain.domains.order.exception.NotPendingOrderException;
import band.gosrock.domain.domains.order.exception.OrderLineNotFountException;
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
import javax.persistence.PostPersist;
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
    @Column(nullable = false)
    private Long userId;

    // 토스페이먼츠용 주문번호
    @Column(nullable = false)
    private String uuid;

    private String orderNo;

    @Column(nullable = false)
    private String orderName;

    // 결제 방식 ( 토스 승인 이후 저장 )
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod = PaymentMethod.DEFAULT;
    // 토스 결제 승인후 결제 긁힌 시간
    private LocalDateTime approvedAt;

    // 결제 공급자 정보 ex 카카오페이
    private String paymentProvider;

    // 영수증 주소
    private String receiptUrl;

    // 세금
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "vat_amount"))
    private Money vat;

    // 결제 정보
    @Embedded private PaymentInfo totalPaymentInfo;

    // 주문 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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

    @PostPersist
    public void createOrderNo() {
        this.orderNo = "R" + Long.sum(NO_START_NUMBER, this.id);
    }

    @Builder
    public Order(
            Long userId,
            String OrderName,
            List<OrderLineItem> orderLineItems,
            OrderStatus orderStatus) {
        this.userId = userId;
        this.orderName = OrderName;
        this.orderLineItems.addAll(orderLineItems);
        this.orderStatus = orderStatus;
    }

    /**
     * 카드, 간편결제등 토스 요청 과정이 필요한 결제를 생성합니다.
     *
     * @param userId
     * @param cart
     * @return
     */
    public static Order createPaymentOrder(Long userId, Cart cart) {
        return orderBaseBuilder(userId, cart).orderStatus(OrderStatus.PENDING_PAYMENT).build();
    }

    /**
     * 승인 결제인 주문을 생성합니다.
     *
     * @param userId
     * @param cart
     * @return
     */
    public static Order createApproveOrder(Long userId, Cart cart) {
        return orderBaseBuilder(userId, cart).orderStatus(OrderStatus.PENDING_APPROVE).build();
    }

    private static OrderBuilder orderBaseBuilder(Long userId, Cart cart) {
        List<OrderLineItem> orderLineItems =
                cart.getCartLineItems().stream().map(OrderLineItem::from).toList();
        return Order.builder()
                .userId(userId)
                .OrderName(cart.getCartName())
                .orderLineItems(orderLineItems);
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

    /** totalPaymentInfo 를 업데이트 합니다. */
    public void calculatePaymentInfo() {
        totalPaymentInfo =
                PaymentInfo.builder()
                        .discountAmount(getTotalDiscountPrice())
                        .paymentAmount(getTotalPaymentPrice())
                        .supplyAmount(getTotalSupplyPrice())
                        .build();
    }

    /**
     * 오더의 결제를 승인 합니다.
     *
     * @param currentUserId
     * @param requestAmount
     */
    public void confirmPaymentOrder(Long currentUserId, Money requestAmount) {
        if (!userId.equals(currentUserId)) {
            throw NotMyOrderException.EXCEPTION;
        }
        if (!getTotalPaymentPrice().equals(requestAmount)) {
            throw InvalidOrderException.EXCEPTION;
        }
        if (!orderStatus.equals(OrderStatus.PENDING_PAYMENT)) {
            throw NotPendingOrderException.EXCEPTION;
        }
        // TODO: 재고량 비교 필요?
        orderStatus = OrderStatus.CONFIRM;
    }

    /**
     * 토스 결제 승인 이후 넘어온 응답값을 바탕으로 vat 등 결제 정보를 업데이트 합니다.
     *
     * @param approvedAt
     * @param paymentMethod
     * @param vat
     */
    public void afterPaymentAddInfo(
            LocalDateTime approvedAt, PaymentMethod paymentMethod, Money vat,String provider,String receiptUrl) {
        this.approvedAt = approvedAt;
        this.paymentMethod = paymentMethod;
        this.vat = vat;
        this.paymentProvider = provider;
        this.receiptUrl = receiptUrl;
    }

    /**
     * 상품의 환불 정보를 가져옵니다. 원래는 오더 라인 마다 쿠폰이 각각 적용되고, 환불 도 가능해야하지만 한이벤트에서만 주문이 가능한 현 기획에 따라 오더라인에 있는
     * 환불정보(환불정보는 이벤트에 따름) 첫번째꺼를 환불 정보로 노출 시켰습니다.
     *
     * @return
     */
    public RefundInfoVo getTotalRefundInfo() {
        OrderLineItem orderLineItem =
                orderLineItems.stream()
                        .findFirst()
                        .orElseThrow(() -> OrderLineNotFountException.EXCEPTION);
        return orderLineItem.getRefundInfo();
    }

    /**
     * 쿠폰의 이름을 가져옵니다
     *
     * @default 사용하지않음
     * @return
     */
    public String getCouponName() {
        if (issuedCoupon != null) {
            return issuedCoupon.getCouponName();
        }
        return "사용하지 않음";
    }

    public void validPgAndOrderAmountIsEqual(Money pgAmount) {
        if (!pgAmount.equals(getTotalPaymentPrice())) {
            throw InvalidOrderException.EXCEPTION;
        }
    }
}
