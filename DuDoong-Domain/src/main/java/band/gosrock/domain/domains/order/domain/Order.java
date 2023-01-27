package band.gosrock.domain.domains.order.domain;

import static band.gosrock.common.consts.DuDoongStatic.NO_START_NUMBER;

import band.gosrock.domain.common.aop.domainEvent.Events;
import band.gosrock.domain.common.events.order.CreateOrderEvent;
import band.gosrock.domain.common.events.order.DoneOrderEvent;
import band.gosrock.domain.common.events.order.WithDrawOrderEvent;
import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import band.gosrock.domain.domains.order.domain.validator.OrderValidator;
import band.gosrock.domain.domains.order.exception.InvalidOrderException;
import band.gosrock.domain.domains.order.exception.NotPaymentOrderException;
import band.gosrock.domain.domains.order.exception.OrderLineNotFountException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
import javax.persistence.PostPersist;
import javax.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

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

    // 결제 대행사 정보 ( 토스 승인 이후 저장 )
    @Embedded private PgPaymentInfo pgPaymentInfo = PgPaymentInfo.empty();

    // 결제 완료 된시간 승인 결제등.
    private LocalDateTime approvedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderMethod orderMethod;
    // 결제 정보
    @Embedded private PaymentInfo totalPaymentInfo;

    // 주문 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus = OrderStatus.READY;

    @Embedded private OrderCouponVo orderCouponVo = OrderCouponVo.empty();

    // 단방향 oneToMany 매핑
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private List<OrderLineItem> orderLineItems = new ArrayList<>();

    @PrePersist
    public void addUUID() {
        this.uuid = UUID.randomUUID().toString();
    }

    @PostPersist
    public void createOrder() {
        this.orderNo = "R" + Long.sum(NO_START_NUMBER, this.id);
        Events.raise(CreateOrderEvent.from(this));
    }

    /** ---------------------------- 생성 관련 메서드 ---------------------------------- */
    @Builder
    public Order(
            Long userId,
            String orderName,
            List<OrderLineItem> orderLineItems,
            OrderStatus orderStatus,
            OrderMethod orderMethod,
            OrderCouponVo orderCouponVo) {
        this.userId = userId;
        this.orderName = orderName;
        this.orderLineItems.addAll(orderLineItems);
        this.orderStatus = orderStatus;
        this.orderMethod = orderMethod;
        this.orderCouponVo = orderCouponVo;
    }

    /** 카드, 간편결제등 토스 요청 과정이 필요한 결제를 생성합니다. */
    public static Order createPaymentOrder(Long userId, Cart cart) {

        return Order.builder()
                .userId(userId)
                .orderName(cart.getCartName())
                .orderLineItems(getOrderLineItems(cart))
                .orderStatus(OrderStatus.PENDING_PAYMENT)
                .orderMethod(OrderMethod.PAYMENT)
                .build();
    }

    /** 승인 결제인 주문을 생성합니다. */
    public static Order createApproveOrder(Long userId, Cart cart) {
        return Order.builder()
                .userId(userId)
                .orderName(cart.getCartName())
                .orderLineItems(getOrderLineItems(cart))
                .orderStatus(OrderStatus.PENDING_APPROVE)
                .orderMethod(OrderMethod.APPROVAL)
                .build();
    }

    public static Order createWithCoupon(Long userId, Cart cart, IssuedCoupon coupon) {
        // 선착순 결제라면 결제 가능한 금액이 있어야 쿠폰 적용이 가능하다.
        if (!cart.getItemType().isFCFS() || !cart.isNeedPaid()) {
            throw InvalidOrderException.EXCEPTION;
        }

        Money supplyAmount = cart.getTotalPrice();
        OrderCouponVo couponVo = OrderCouponVo.of(coupon, supplyAmount);
        couponVo.validMinimumPaymentAmount(supplyAmount);

        return Order.builder()
                .userId(userId)
                .orderName(cart.getCartName())
                .orderLineItems(getOrderLineItems(cart))
                .orderStatus(OrderStatus.PENDING_PAYMENT)
                .orderMethod(OrderMethod.PAYMENT)
                .orderCouponVo(couponVo)
                .build();
    }

    @NotNull
    private static List<OrderLineItem> getOrderLineItems(Cart cart) {
        List<OrderLineItem> orderLineItems =
                cart.getCartLineItems().stream().map(OrderLineItem::from).toList();
        return orderLineItems;
    }

    public static Order create(Long userId, Cart cart) {
        // 선착순 결제라면
        if (cart.getItemType().isFCFS()) {
            return createPaymentOrder(userId, cart);
        }
        // 선착순이 아니라면? 승인 방식임. 승인방식의 결제가 필요한 상황은 지원하지않음.
        if (cart.isNeedPaid()) {
            throw InvalidOrderException.EXCEPTION;
        }
        return createApproveOrder(userId, cart);
    }
    /** ---------------------------- 커맨드 메서드 ---------------------------------- */

    /** totalPaymentInfo 를 업데이트 합니다. */
    public void calculatePaymentInfo() {
        totalPaymentInfo =
                PaymentInfo.builder()
                        .discountAmount(getTotalDiscountPrice())
                        .paymentAmount(getTotalPaymentPrice())
                        .supplyAmount(getTotalSupplyPrice())
                        .build();
    }

    /** 결제 방식의 주문을 승인 합니다. */
    public void confirmPayment(
            LocalDateTime approvedAt, PgPaymentInfo pgPaymentInfo, OrderValidator orderValidator) {
        orderValidator.validCanConfirmPayment(this);
        orderStatus = OrderStatus.CONFIRM;
        this.approvedAt = approvedAt;
        this.pgPaymentInfo = pgPaymentInfo;
        Events.raise(DoneOrderEvent.from(this));
    }

    /** 승인 방식의 주문을 승인합니다. */
    public void approve(OrderValidator orderValidator) {
        orderValidator.validCanApprovalOrder(this);
        this.approvedAt = LocalDateTime.now();
        this.orderStatus = OrderStatus.APPROVED;
        Events.raise(DoneOrderEvent.from(this));
    }

    /** 선착순 방식의 0원 결제입니다. */
    public void freeConfirm(OrderValidator orderValidator) {
        orderValidator.validCanFreeConfirm(this);
        this.approvedAt = LocalDateTime.now();
        this.orderStatus = OrderStatus.APPROVED;
        Events.raise(DoneOrderEvent.from(this));
    }

    /** 관리자가 주문을 취소 시킵니다 */
    public void cancel(OrderValidator orderValidator) {
        orderValidator.validCanCancel(this);
        this.orderStatus = OrderStatus.CANCELED;
        Events.raise(WithDrawOrderEvent.from(this));
    }

    /** 사용자가 주문을 환불 시킵니다. */
    public void refund(OrderValidator orderValidator) {
        orderValidator.validCanRefund(this);
        this.orderStatus = OrderStatus.REFUND;
        Events.raise(WithDrawOrderEvent.from(this));
    }

    /** 결제 실패 된 주문입니다 */
    public void fail() {
        this.orderStatus = OrderStatus.FAILED;
    }

    /** ---------------------------- 조회용 메서드 ---------------------------------- */
    /** 결제 방식의 paymentKey를 가져옵니다. */
    public String getPaymentKey() {
        if (Objects.isNull(this.pgPaymentInfo)) {
            throw NotPaymentOrderException.EXCEPTION;
        }
        return this.pgPaymentInfo.getPaymentKey();
    }

    /**
     * 쿠폰의 이름을 가져옵니다
     *
     * @default 사용하지않음
     */
    public String getCouponName() {
        return orderCouponVo.getName();
    }

    /** 총 공급가액을 가져옵니다. */
    public Money getTotalSupplyPrice() {
        return orderLineItems.stream()
                .map(OrderLineItem::getTotalOrderLinePrice)
                .reduce(Money.ZERO, Money::plus);
    }

    /** 총 결제금액을 가져옵니다. 공급가액 - 할인가 */
    public Money getTotalPaymentPrice() {
        return getTotalSupplyPrice().minus(getTotalDiscountPrice());
    }
    /** 총 할인금액을 가져옵니다. */
    public Money getTotalDiscountPrice() {
        return orderCouponVo.getDiscountAmount();
    }

    public Boolean hasCoupon() {
        return !orderCouponVo.isDefault();
    }

    /**
     * 상품의 환불 정보를 가져옵니다. 원래는 오더 라인 마다 쿠폰이 각각 적용되고, 환불 도 가능해야하지만 한이벤트에서만 주문이 가능한 현 기획에 따라 오더라인에 있는
     * 환불정보(환불정보는 이벤트에 따름) 첫번째꺼를 환불 정보로 노출 시켰습니다.
     *
     * @return
     */
    //    public RefundInfoVo getTotalRefundInfo() {
    //        return getOrderLineItem().getRefundInfo();
    //    }

    private OrderLineItem getOrderLineItem() {
        return orderLineItems.stream()
                .findFirst()
                .orElseThrow(() -> OrderLineNotFountException.EXCEPTION);
    }

    public Long getItemId() {
        return getOrderLineItem().getItemId();
    }

    public Long getItemGroupId() {
        return getOrderLineItem().getItemGroupId();
    }

    /** 주문에서 티켓 상품의 타입을 반환합니다. */
    //    public TicketType getItemType() {
    //        return getItem().getType();
    //    }

    /** 결제가 필요한 오더인지 반환합니다. */
    public Boolean isNeedPaid() {
        // 결제 여부는 총 결제금액으로 정함
        return Money.ZERO.isLessThan(getTotalPaymentPrice());
    }

    /** 결제 수단 정보를 가져옵니다. */
    public String getMethod() {
        if (orderMethod.equals(OrderMethod.APPROVAL)) return OrderMethod.APPROVAL.getKr();
        return pgPaymentInfo.getPaymentMethod().getKr();
    }

    /** 결제 공급자 정보를 가져옵니다. */
    public String getProvider() {
        return this.pgPaymentInfo.getPaymentProvider();
    }

    /** 결제 공급자 정보를 가져옵니다. */
    public String getReceiptUrl() {
        return this.pgPaymentInfo.getReceiptUrl();
    }

    /** PG 사를 통해 결제가 된 주문인지 반환합니다. */
    public Boolean isPaid() {
        return isNeedPaid();
    }
}
