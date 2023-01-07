package band.gosrock.domain.domains.order.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.domains.user.domain.UserId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Order extends BaseTimeEntity {
    @EmbeddedId private OrderId id;

    @Column(nullable = false)
    private UserId userId;

    @Column(nullable = false)
    private String uuid;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Embedded private PaymentInfo totalPaymentInfo;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    private LocalDateTime paymentAt;

    // 단방향 oneToMany 매핑
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private List<OrderLineItem> orderLineItems = new ArrayList<>();

    @PrePersist
    public void addUUID() {
        this.uuid = UUID.randomUUID().toString();
    }
}
