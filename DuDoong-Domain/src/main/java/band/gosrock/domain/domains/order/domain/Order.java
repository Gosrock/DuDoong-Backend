package band.gosrock.domain.domains.order.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
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

    private Long userId;

    @Column(nullable = false)
    private String uuid;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Embedded private PaymentInfo totalPaymentInfo;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    private LocalDateTime paymentAt;

//         단방향 oneToMany 매핑
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private List<OrderLineItem> orderLineItems = new ArrayList<>();

    @Builder
    public Order(
            Long userId,
            PaymentMethod paymentMethod,
            PaymentInfo totalPaymentInfo,
            OrderStatus orderStatus,
            LocalDateTime paymentAt,
            List<OrderLineItem> orderLineItems) {
        this.userId = userId;
        this.paymentMethod = paymentMethod;
        this.totalPaymentInfo = totalPaymentInfo;
        this.orderStatus = orderStatus;
        this.paymentAt = paymentAt;
//        this.orderLineItems.addAll(orderLineItems);
    }

    public static Order createOrder(
            Long userId,
            PaymentMethod paymentMethod,
            PaymentInfo totalPaymentInfo,
            List<OrderLineItem> orderLineItems) {
        Order order =
                Order.builder()
                        .paymentMethod(paymentMethod)
                        .userId(userId)
                        .totalPaymentInfo(totalPaymentInfo)
                        .orderLineItems(orderLineItems)
                        .build();
        return order;
    }

    @PrePersist
    public void addUUID() {
        this.uuid = UUID.randomUUID().toString();
    }
}
