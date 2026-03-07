package band.gosrock.domain.domains.order.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPaymentInfo is a Querydsl query type for PaymentInfo
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QPaymentInfo extends BeanPath<PaymentInfo> {

    private static final long serialVersionUID = -2029184580L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPaymentInfo paymentInfo = new QPaymentInfo("paymentInfo");

    public final band.gosrock.domain.common.vo.QMoney discountAmount;

    public final band.gosrock.domain.common.vo.QMoney paymentAmount;

    public final band.gosrock.domain.common.vo.QMoney supplyAmount;

    public QPaymentInfo(String variable) {
        this(PaymentInfo.class, forVariable(variable), INITS);
    }

    public QPaymentInfo(Path<? extends PaymentInfo> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPaymentInfo(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPaymentInfo(PathMetadata metadata, PathInits inits) {
        this(PaymentInfo.class, metadata, inits);
    }

    public QPaymentInfo(Class<? extends PaymentInfo> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.discountAmount = inits.isInitialized("discountAmount") ? new band.gosrock.domain.common.vo.QMoney(forProperty("discountAmount")) : null;
        this.paymentAmount = inits.isInitialized("paymentAmount") ? new band.gosrock.domain.common.vo.QMoney(forProperty("paymentAmount")) : null;
        this.supplyAmount = inits.isInitialized("supplyAmount") ? new band.gosrock.domain.common.vo.QMoney(forProperty("supplyAmount")) : null;
    }

}

