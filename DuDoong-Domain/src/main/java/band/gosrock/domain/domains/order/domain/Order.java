package band.gosrock.domain.domains.order.domain;

import static band.gosrock.common.consts.DuDoongStatic.NO_START_NUMBER;

import band.gosrock.domain.common.aop.domainEvent.Events;
import band.gosrock.domain.common.events.order.DoneOrderEvent;
import band.gosrock.domain.common.events.order.WithDrawOrderEvent;
import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.RefundInfoVo;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import band.gosrock.domain.domains.order.exception.InvalidOrderException;
import band.gosrock.domain.domains.order.exception.NotApprovalOrderException;
import band.gosrock.domain.domains.order.exception.NotOwnerOrderException;
import band.gosrock.domain.domains.order.exception.NotPaymentOrderException;
import band.gosrock.domain.domains.order.exception.NotRefundAvailableDateOrderException;
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

    // 결제 대행사 정보 ( 토스 승인 이후 저장 )
    @Embedded private PgPaymentInfo pgPaymentInfo;

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

    /** ---------------------------- 생성 관련 메서드 ---------------------------------- */
    @Builder
    public Order(
            Long userId,
            String OrderName,
            List<OrderLineItem> orderLineItems,
            OrderStatus orderStatus,
            OrderMethod orderMethod) {
        this.userId = userId;
        this.orderName = OrderName;
        this.orderLineItems.addAll(orderLineItems);
        this.orderStatus = orderStatus;
        this.orderMethod = orderMethod;
    }

    /** 카드, 간편결제등 토스 요청 과정이 필요한 결제를 생성합니다. */
    public static Order createPaymentOrder(Long userId, Cart cart) {
        List<OrderLineItem> orderLineItems =
                cart.getCartLineItems().stream().map(OrderLineItem::from).toList();
        return Order.builder()
                .userId(userId)
                .OrderName(cart.getCartName())
                .orderLineItems(orderLineItems)
                .orderStatus(OrderStatus.PENDING_PAYMENT)
                .orderMethod(OrderMethod.PAYMENT)
                .build();
    }

    /** 승인 결제인 주문을 생성합니다. */
    public static Order createApproveOrder(Long userId, Cart cart) {
        List<OrderLineItem> orderLineItems =
                cart.getCartLineItems().stream().map(OrderLineItem::from).toList();
        return Order.builder()
                .userId(userId)
                .OrderName(cart.getCartName())
                .orderLineItems(orderLineItems)
                .orderStatus(OrderStatus.PENDING_APPROVE)
                .orderMethod(OrderMethod.APPROVAL)
                .build();
    }

    public static Order createOrder(Long userId, Cart cart) {
        if (cart.isNeedPayment()) return createPaymentOrder(userId, cart);
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
            Money pgAmount, LocalDateTime approvedAt, PgPaymentInfo pgPaymentInfo) {
        // TODO: 재고량 비교 필요?
        validCanConfirmPayment(pgAmount);
        validPgAndOrderAmountIsEqual(pgAmount);
        orderStatus = OrderStatus.CONFIRM;
        this.approvedAt = approvedAt;
        this.pgPaymentInfo = pgPaymentInfo;
        Events.raise(DoneOrderEvent.of(this.uuid,this.userId));
    }

    /** 승인 방식의 주문을 승인합니다. */
    public void approve() {
        if (isNeedPayment()) {
            throw NotApprovalOrderException.EXCEPTION;
        }
        orderStatus.validCanApprove();
        // TODO: 재고량 비교 필요?
        this.approvedAt = LocalDateTime.now();
        this.orderStatus = OrderStatus.APPROVED;
        Events.raise(DoneOrderEvent.of(this.uuid,this.userId));
    }

    /** 관리자가 주문을 취소 시킵니다 */
    public void cancel() {
        orderStatus.validCanCancel();
        validCanRefundDate();
        this.orderStatus = OrderStatus.CANCELED;
        Events.raise(WithDrawOrderEvent.of(this.uuid,this.userId));
    }

    /** 사용자가 주문을 환불 시킵니다. */
    public void refund() {
        orderStatus.validCanRefund();
        validCanRefundDate();
        this.orderStatus = OrderStatus.REFUND;
        Events.raise(WithDrawOrderEvent.of(this.uuid,this.userId));
    }

    /** ---------------------------- 검증 메서드 ---------------------------------- */

    /** PG 사를 통한 결제 대금이 주문의 가격과 동일한지 검증합니다. */
    public void validPgAndOrderAmountIsEqual(Money pgAmount) {
        if (!pgAmount.equals(getTotalPaymentPrice())) {
            throw InvalidOrderException.EXCEPTION;
        }
    }
    /** 주문에대한 주인인지 검증합니다. */
    public void validOwner(Long currentUserId) {
        if (!userId.equals(currentUserId)) {
            throw NotOwnerOrderException.EXCEPTION;
        }
    }
    /** 결제 방식의 주문을 승인할수있는지 확인합니다. */
    public void validCanConfirmPayment(Money requestAmount) {
        if (!getTotalPaymentPrice().equals(requestAmount)) {
            throw InvalidOrderException.EXCEPTION;
        }
        if (!isNeedPayment()) {
            throw NotPaymentOrderException.EXCEPTION;
        }
        orderStatus.validCanPaymentConfirm();
    }

    public void validCanRefundDate() {
        if (!canRefundDate()) {
            throw NotRefundAvailableDateOrderException.EXCEPTION;
        }
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
        if (issuedCoupon != null) {
            return issuedCoupon.getCouponName();
        }
        return "사용하지 않음";
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
        if (issuedCoupon != null) {
            return issuedCoupon.getDiscountAmount(getTotalSupplyPrice());
        }
        return Money.ZERO;
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

    /** 결제가 필요한 오더인지 반환합니다. */
    public Boolean isNeedPayment() {
        return this.orderLineItems.stream()
                .map(OrderLineItem::isNeedPayment)
                .reduce(Boolean.FALSE, (Boolean::logicalOr));
    }

    /** 결제 수단 정보를 가져옵니다. */
    public String getMethod() {
        if (this.orderMethod.equals(OrderMethod.APPROVAL)) return OrderMethod.APPROVAL.getKr();
        return this.pgPaymentInfo.getPaymentMethod().getKr();
    }

    /** 결제 공급자 정보를 가져옵니다. */
    public String getProvider() {
        if (this.orderMethod.equals(OrderMethod.APPROVAL)) return null;
        return this.pgPaymentInfo.getPaymentProvider();
    }

    /** 결제 공급자 정보를 가져옵니다. */
    public String getReceiptUrl() {
        if (this.orderMethod.equals(OrderMethod.APPROVAL)) return null;
        return this.pgPaymentInfo.getReceiptUrl();
    }

    public Boolean isMethodPayment() {
        return orderMethod.isPayment();
    }

    public Boolean canRefundDate() {
        return this.orderLineItems.stream()
                .map(OrderLineItem::canRefund)
                .reduce(Boolean.TRUE, (Boolean::logicalAnd));
    }
}
