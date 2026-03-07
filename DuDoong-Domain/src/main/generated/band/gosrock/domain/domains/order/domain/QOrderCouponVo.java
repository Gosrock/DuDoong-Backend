package band.gosrock.domain.domains.order.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrderCouponVo is a Querydsl query type for OrderCouponVo
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QOrderCouponVo extends BeanPath<OrderCouponVo> {

    private static final long serialVersionUID = 180737749L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrderCouponVo orderCouponVo = new QOrderCouponVo("orderCouponVo");

    public final NumberPath<Long> couponId = createNumber("couponId", Long.class);

    public final band.gosrock.domain.common.vo.QMoney discountAmount;

    public final StringPath name = createString("name");

    public QOrderCouponVo(String variable) {
        this(OrderCouponVo.class, forVariable(variable), INITS);
    }

    public QOrderCouponVo(Path<? extends OrderCouponVo> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrderCouponVo(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrderCouponVo(PathMetadata metadata, PathInits inits) {
        this(OrderCouponVo.class, metadata, inits);
    }

    public QOrderCouponVo(Class<? extends OrderCouponVo> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.discountAmount = inits.isInitialized("discountAmount") ? new band.gosrock.domain.common.vo.QMoney(forProperty("discountAmount")) : null;
    }

}

