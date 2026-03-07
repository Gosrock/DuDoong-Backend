package band.gosrock.domain.domains.coupon.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QIssuedCoupon is a Querydsl query type for IssuedCoupon
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QIssuedCoupon extends EntityPathBase<IssuedCoupon> {

    private static final long serialVersionUID = 934469381L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QIssuedCoupon issuedCoupon = new QIssuedCoupon("issuedCoupon");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    public final QCouponCampaign couponCampaign;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final BooleanPath usageStatus = createBoolean("usageStatus");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QIssuedCoupon(String variable) {
        this(IssuedCoupon.class, forVariable(variable), INITS);
    }

    public QIssuedCoupon(Path<? extends IssuedCoupon> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QIssuedCoupon(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QIssuedCoupon(PathMetadata metadata, PathInits inits) {
        this(IssuedCoupon.class, metadata, inits);
    }

    public QIssuedCoupon(Class<? extends IssuedCoupon> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.couponCampaign = inits.isInitialized("couponCampaign") ? new QCouponCampaign(forProperty("couponCampaign"), inits.get("couponCampaign")) : null;
    }

}

