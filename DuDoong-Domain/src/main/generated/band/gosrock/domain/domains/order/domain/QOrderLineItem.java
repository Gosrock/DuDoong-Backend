package band.gosrock.domain.domains.order.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrderLineItem is a Querydsl query type for OrderLineItem
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrderLineItem extends EntityPathBase<OrderLineItem> {

    private static final long serialVersionUID = 1739385469L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrderLineItem orderLineItem = new QOrderLineItem("orderLineItem");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QOrderItemVo orderItem;

    public final ListPath<OrderOptionAnswer, QOrderOptionAnswer> orderOptionAnswers = this.<OrderOptionAnswer, QOrderOptionAnswer>createList("orderOptionAnswers", OrderOptionAnswer.class, QOrderOptionAnswer.class, PathInits.DIRECT2);

    public final NumberPath<Long> quantity = createNumber("quantity", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QOrderLineItem(String variable) {
        this(OrderLineItem.class, forVariable(variable), INITS);
    }

    public QOrderLineItem(Path<? extends OrderLineItem> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrderLineItem(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrderLineItem(PathMetadata metadata, PathInits inits) {
        this(OrderLineItem.class, metadata, inits);
    }

    public QOrderLineItem(Class<? extends OrderLineItem> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.orderItem = inits.isInitialized("orderItem") ? new QOrderItemVo(forProperty("orderItem"), inits.get("orderItem")) : null;
    }

}

