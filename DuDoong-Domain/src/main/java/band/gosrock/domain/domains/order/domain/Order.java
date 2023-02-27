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
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkOrderInfo;
import band.gosrock.infrastructure.config.mail.dto.EmailOrderInfo;
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

    // 관리자에서 주문 목록 불러올려면 event 아이디 저장 필요
    @Column(nullable = false)
    private Long eventId;

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

    // 철회된 시간
    private LocalDateTime withDrawAt;

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
            Long eventId) {
        this.userId = userId;
        this.orderName = orderName;
        this.orderLineItems.addAll(orderLineItems);
        this.orderStatus = orderStatus;
        this.orderMethod = orderMethod;
        this.eventId = eventId;
    }

    /** 카드, 간편결제등 토스 요청 과정이 필요한 결제를 생성합니다. */
    public static Order createPaymentOrder(
            Long userId, Cart cart, TicketItem item, OrderValidator orderValidator) {
        Order order =
                Order.builder()
                        .userId(userId)
                        .orderName(cart.getCartName())
                        .orderLineItems(getOrderLineItems(cart, item))
                        .orderStatus(OrderStatus.PENDING_PAYMENT)
                        .orderMethod(OrderMethod.PAYMENT)
                        .eventId(item.getEventId())
                        .build();
        orderValidator.validCanCreate(order);
        order.calculatePaymentInfo();
        return order;
    }

    /** 승인 결제인 주문을 생성합니다. */
    public static Order createApproveOrder(
            Long userId, Cart cart, TicketItem item, OrderValidator orderValidator) {
        Order order =
                Order.builder()
                        .userId(userId)
                        .orderName(cart.getCartName())
                        .orderLineItems(getOrderLineItems(cart, item))
                        .orderStatus(OrderStatus.PENDING_APPROVE)
                        .orderMethod(OrderMethod.APPROVAL)
                        .eventId(item.getEventId())
                        .build();
        orderValidator.validCanCreate(order);
        orderValidator.validApproveStatePurchaseLimit(order);
        order.calculatePaymentInfo();
        return order;
    }

    public static Order createPaymentOrderWithCoupon(
            Long userId,
            Cart cart,
            TicketItem item,
            IssuedCoupon coupon,
            OrderValidator orderValidator) {
        // 선착순 결제라면 결제 가능한 금액이 있어야 쿠폰 적용이 가능하다.
        if (!item.isFCFS() || !cart.isNeedPaid()) {
            throw InvalidOrderException.EXCEPTION;
        }

        Money supplyAmount = cart.getTotalPrice();
        OrderCouponVo couponVo = OrderCouponVo.of(coupon, supplyAmount);
        couponVo.validMinimumPaymentAmount(supplyAmount);
        Order order = createPaymentOrder(userId, cart, item, orderValidator);
        order.attachCoupon(couponVo);
        order.calculatePaymentInfo();
        return order;
    }

    @NotNull
    private static List<OrderLineItem> getOrderLineItems(Cart cart, TicketItem item) {
        return cart.getCartLineItems().stream()
                .map(cartLineItem -> OrderLineItem.of(cartLineItem, item))
                .toList();
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
        issueDoneOrderEvent();
        orderValidator.validCanConfirmPayment(this);
        orderStatus = OrderStatus.CONFIRM;
        this.approvedAt = approvedAt;
        this.pgPaymentInfo = pgPaymentInfo;
    }

    /** 승인 방식의 주문을 승인합니다. */
    public void approve(OrderValidator orderValidator) {
        issueDoneOrderEvent();
        orderValidator.validCanApproveOrder(this);
        this.approvedAt = LocalDateTime.now();
        this.orderStatus = OrderStatus.APPROVED;
    }

    /** 선착순 방식의 0원 결제입니다. */
    public void freeConfirm(Long currentUserId, OrderValidator orderValidator) {
        orderValidator.validOwner(this, currentUserId);
        issueDoneOrderEvent();
        orderValidator.validCanFreeConfirm(this);
        this.approvedAt = LocalDateTime.now();
        this.orderStatus = OrderStatus.APPROVED;
    }

    /**
     * 주문 상태가 완료될수 있는 상태일 때 돈 오더 이벤트를 발생시킵니다.
     */
    private void issueDoneOrderEvent() {
        if (orderStatus.isCanDone()) {
            Events.raise(DoneOrderEvent.from(this));
        }
    }

    /** 관리자가 주문을 취소 시킵니다 */
    public void cancel(OrderValidator orderValidator) {
        orderValidator.validCanCancel(this);
        this.orderStatus = OrderStatus.CANCELED;
        this.withDrawAt = LocalDateTime.now();
        Events.raise(WithDrawOrderEvent.from(this));
    }

    /** 사용자가 주문을 환불 시킵니다. */
    public void refund(Long currentUserId, OrderValidator orderValidator) {
        orderValidator.validOwner(this, currentUserId);
        orderValidator.validCanRefund(this);
        this.orderStatus = OrderStatus.REFUND;
        this.withDrawAt = LocalDateTime.now();
        Events.raise(WithDrawOrderEvent.from(this));
    }

    /** 결제 실패 된 주문입니다 */
    public void fail() {
        this.orderStatus = OrderStatus.FAILED;
    }

    /** 쿠폰을 붙입니다. */
    public void attachCoupon(OrderCouponVo orderCouponVo) {
        this.orderCouponVo = orderCouponVo;
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

    /** 쿠폰이 적용된 주문인지 조회합니다. */
    public Boolean hasCoupon() {
        return !orderCouponVo.isDefault();
    }

    /** 오더라인목록의 한 요소를 가져옵니다. */
    private OrderLineItem getOrderLineItem() {
        return orderLineItems.stream()
                .findFirst()
                .orElseThrow(() -> OrderLineNotFountException.EXCEPTION);
    }

    /** 아이템의 아이디를 가져옵니다. */
    public Long getItemId() {
        return getOrderLineItem().getItemId();
    }

    /** 아이템의 그룹 아이디 ( 이벤트 아이디 ) 를 가져옵니다. */
    public Long getItemGroupId() {
        return getOrderLineItem().getItemGroupId();
    }

    /** 결제가 필요한 오더인지 반환합니다. */
    public Boolean isNeedPaid() {
        // 결제 여부는 총 결제금액으로 정함
        return Money.ZERO.isLessThan(getTotalPaymentPrice()) && orderMethod.isPayment();
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

    public Boolean isDudoongTicketOrder() {
        return getTotalPaymentPrice().isGreaterThan(Money.ZERO)
                && orderMethod == OrderMethod.APPROVAL;
    }

    public List<Long> getDistinctItemIds() {
        return this.orderLineItems.stream().map(OrderLineItem::getItemId).distinct().toList();
    }

    public Long getTotalQuantity() {
        return orderLineItems.stream().map(OrderLineItem::getQuantity).reduce(0L, Long::sum);
    }

    public EmailOrderInfo toEmailOrderInfo() {
        return new EmailOrderInfo(
                orderName, getTotalQuantity(), getTotalPaymentPrice().toString(), getCreatedAt());
    }

    public AlimTalkOrderInfo toAlimTalkOrderInfo() {
        return new AlimTalkOrderInfo(
                orderName, getTotalQuantity(), getTotalPaymentPrice().toString(), getCreatedAt());
    }
}
