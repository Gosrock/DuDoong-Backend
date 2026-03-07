package band.gosrock.domain.domains.order.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrder is a Querydsl query type for Order
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrder extends EntityPathBase<Order> {

    private static final long serialVersionUID = 1578473270L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrder order = new QOrder("order1");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    public final DateTimePath<java.time.LocalDateTime> approvedAt = createDateTime("approvedAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> eventId = createNumber("eventId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QOrderCouponVo orderCouponVo;

    public final ListPath<OrderLineItem, QOrderLineItem> orderLineItems = this.<OrderLineItem, QOrderLineItem>createList("orderLineItems", OrderLineItem.class, QOrderLineItem.class, PathInits.DIRECT2);

    public final EnumPath<OrderMethod> orderMethod = createEnum("orderMethod", OrderMethod.class);

    public final StringPath orderName = createString("orderName");

    public final StringPath orderNo = createString("orderNo");

    public final EnumPath<OrderStatus> orderStatus = createEnum("orderStatus", OrderStatus.class);

    public final QPgPaymentInfo pgPaymentInfo;

    public final QPaymentInfo totalPaymentInfo;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final StringPath uuid = createString("uuid");

    public final DateTimePath<java.time.LocalDateTime> withDrawAt = createDateTime("withDrawAt", java.time.LocalDateTime.class);

    public QOrder(String variable) {
        this(Order.class, forVariable(variable), INITS);
    }

    public QOrder(Path<? extends Order> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrder(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrder(PathMetadata metadata, PathInits inits) {
        this(Order.class, metadata, inits);
    }

    public QOrder(Class<? extends Order> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.orderCouponVo = inits.isInitialized("orderCouponVo") ? new QOrderCouponVo(forProperty("orderCouponVo"), inits.get("orderCouponVo")) : null;
        this.pgPaymentInfo = inits.isInitialized("pgPaymentInfo") ? new QPgPaymentInfo(forProperty("pgPaymentInfo"), inits.get("pgPaymentInfo")) : null;
        this.totalPaymentInfo = inits.isInitialized("totalPaymentInfo") ? new QPaymentInfo(forProperty("totalPaymentInfo"), inits.get("totalPaymentInfo")) : null;
    }

}

