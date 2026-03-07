package band.gosrock.domain.domains.order.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrderItemVo is a Querydsl query type for OrderItemVo
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QOrderItemVo extends BeanPath<OrderItemVo> {

    private static final long serialVersionUID = 855429122L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrderItemVo orderItemVo = new QOrderItemVo("orderItemVo");

    public final NumberPath<Long> itemGroupId = createNumber("itemGroupId", Long.class);

    public final NumberPath<Long> itemId = createNumber("itemId", Long.class);

    public final StringPath name = createString("name");

    public final band.gosrock.domain.common.vo.QMoney price;

    public QOrderItemVo(String variable) {
        this(OrderItemVo.class, forVariable(variable), INITS);
    }

    public QOrderItemVo(Path<? extends OrderItemVo> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrderItemVo(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrderItemVo(PathMetadata metadata, PathInits inits) {
        this(OrderItemVo.class, metadata, inits);
    }

    public QOrderItemVo(Class<? extends OrderItemVo> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.price = inits.isInitialized("price") ? new band.gosrock.domain.common.vo.QMoney(forProperty("price")) : null;
    }

}

