package band.gosrock.domain.domains.coupon.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCouponCampaign is a Querydsl query type for CouponCampaign
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCouponCampaign extends EntityPathBase<CouponCampaign> {

    private static final long serialVersionUID = -1687517526L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCouponCampaign couponCampaign = new QCouponCampaign("couponCampaign");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    public final EnumPath<ApplyTarget> applyTarget = createEnum("applyTarget", ApplyTarget.class);

    public final StringPath couponCode = createString("couponCode");

    public final QCouponStockInfo couponStockInfo;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final band.gosrock.domain.common.vo.QDateTimePeriod dateTimePeriod;

    public final NumberPath<Long> discountAmount = createNumber("discountAmount", Long.class);

    public final EnumPath<DiscountType> discountType = createEnum("discountType", DiscountType.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<IssuedCoupon, QIssuedCoupon> issuedCoupons = this.<IssuedCoupon, QIssuedCoupon>createList("issuedCoupons", IssuedCoupon.class, QIssuedCoupon.class, PathInits.DIRECT2);

    public final NumberPath<Long> minimumCost = createNumber("minimumCost", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final NumberPath<Long> validTerm = createNumber("validTerm", Long.class);

    public QCouponCampaign(String variable) {
        this(CouponCampaign.class, forVariable(variable), INITS);
    }

    public QCouponCampaign(Path<? extends CouponCampaign> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCouponCampaign(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCouponCampaign(PathMetadata metadata, PathInits inits) {
        this(CouponCampaign.class, metadata, inits);
    }

    public QCouponCampaign(Class<? extends CouponCampaign> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.couponStockInfo = inits.isInitialized("couponStockInfo") ? new QCouponStockInfo(forProperty("couponStockInfo")) : null;
        this.dateTimePeriod = inits.isInitialized("dateTimePeriod") ? new band.gosrock.domain.common.vo.QDateTimePeriod(forProperty("dateTimePeriod")) : null;
    }

}

