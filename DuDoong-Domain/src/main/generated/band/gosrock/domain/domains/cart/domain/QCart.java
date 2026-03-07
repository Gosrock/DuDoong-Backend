package band.gosrock.domain.domains.cart.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCart is a Querydsl query type for Cart
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCart extends EntityPathBase<Cart> {

    private static final long serialVersionUID = -1130677266L;

    public static final QCart cart = new QCart("cart");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    public final ListPath<CartLineItem, QCartLineItem> cartLineItems = this.<CartLineItem, QCartLineItem>createList("cartLineItems", CartLineItem.class, QCartLineItem.class, PathInits.DIRECT2);

    public final StringPath cartName = createString("cartName");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QCart(String variable) {
        super(Cart.class, forVariable(variable));
    }

    public QCart(Path<? extends Cart> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCart(PathMetadata metadata) {
        super(Cart.class, metadata);
    }

}

