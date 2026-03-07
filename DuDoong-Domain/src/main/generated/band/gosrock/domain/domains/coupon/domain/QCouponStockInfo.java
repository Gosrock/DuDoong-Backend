package band.gosrock.domain.domains.coupon.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCouponStockInfo is a Querydsl query type for CouponStockInfo
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QCouponStockInfo extends BeanPath<CouponStockInfo> {

    private static final long serialVersionUID = 536838794L;

    public static final QCouponStockInfo couponStockInfo = new QCouponStockInfo("couponStockInfo");

    public final NumberPath<Long> issuedAmount = createNumber("issuedAmount", Long.class);

    public final NumberPath<Long> remainingAmount = createNumber("remainingAmount", Long.class);

    public QCouponStockInfo(String variable) {
        super(CouponStockInfo.class, forVariable(variable));
    }

    public QCouponStockInfo(Path<? extends CouponStockInfo> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCouponStockInfo(PathMetadata metadata) {
        super(CouponStockInfo.class, metadata);
    }

}

