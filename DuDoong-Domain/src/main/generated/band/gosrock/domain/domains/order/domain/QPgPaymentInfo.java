package band.gosrock.domain.domains.order.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPgPaymentInfo is a Querydsl query type for PgPaymentInfo
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QPgPaymentInfo extends BeanPath<PgPaymentInfo> {

    private static final long serialVersionUID = 766134597L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPgPaymentInfo pgPaymentInfo = new QPgPaymentInfo("pgPaymentInfo");

    public final StringPath paymentKey = createString("paymentKey");

    public final EnumPath<PaymentMethod> paymentMethod = createEnum("paymentMethod", PaymentMethod.class);

    public final StringPath paymentProvider = createString("paymentProvider");

    public final StringPath receiptUrl = createString("receiptUrl");

    public final band.gosrock.domain.common.vo.QMoney vat;

    public QPgPaymentInfo(String variable) {
        this(PgPaymentInfo.class, forVariable(variable), INITS);
    }

    public QPgPaymentInfo(Path<? extends PgPaymentInfo> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPgPaymentInfo(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPgPaymentInfo(PathMetadata metadata, PathInits inits) {
        this(PgPaymentInfo.class, metadata, inits);
    }

    public QPgPaymentInfo(Class<? extends PgPaymentInfo> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.vat = inits.isInitialized("vat") ? new band.gosrock.domain.common.vo.QMoney(forProperty("vat")) : null;
    }

}

