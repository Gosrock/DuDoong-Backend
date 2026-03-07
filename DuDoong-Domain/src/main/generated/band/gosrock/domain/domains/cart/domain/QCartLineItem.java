package band.gosrock.domain.domains.cart.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCartLineItem is a Querydsl query type for CartLineItem
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCartLineItem extends EntityPathBase<CartLineItem> {

    private static final long serialVersionUID = -646031563L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCartLineItem cartLineItem = new QCartLineItem("cartLineItem");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    public final ListPath<CartOptionAnswer, QCartOptionAnswer> cartOptionAnswers = this.<CartOptionAnswer, QCartOptionAnswer>createList("cartOptionAnswers", CartOptionAnswer.class, QCartOptionAnswer.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> itemId = createNumber("itemId", Long.class);

    public final band.gosrock.domain.common.vo.QMoney itemPrice;

    public final NumberPath<Long> quantity = createNumber("quantity", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QCartLineItem(String variable) {
        this(CartLineItem.class, forVariable(variable), INITS);
    }

    public QCartLineItem(Path<? extends CartLineItem> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCartLineItem(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCartLineItem(PathMetadata metadata, PathInits inits) {
        this(CartLineItem.class, metadata, inits);
    }

    public QCartLineItem(Class<? extends CartLineItem> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.itemPrice = inits.isInitialized("itemPrice") ? new band.gosrock.domain.common.vo.QMoney(forProperty("itemPrice")) : null;
    }

}

