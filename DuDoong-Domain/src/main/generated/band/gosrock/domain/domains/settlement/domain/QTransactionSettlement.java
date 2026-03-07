package band.gosrock.domain.domains.settlement.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTransactionSettlement is a Querydsl query type for TransactionSettlement
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTransactionSettlement extends EntityPathBase<TransactionSettlement> {

    private static final long serialVersionUID = 431386544L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTransactionSettlement transactionSettlement = new QTransactionSettlement("transactionSettlement");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    public final DateTimePath<java.time.LocalDateTime> approvedAt = createDateTime("approvedAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> eventId = createNumber("eventId", Long.class);

    public final ListPath<SettlementFeeVo, QSettlementFeeVo> fees = this.<SettlementFeeVo, QSettlementFeeVo>createList("fees", SettlementFeeVo.class, QSettlementFeeVo.class, PathInits.DIRECT2);

    public final band.gosrock.domain.common.vo.QMoney feeSupplyAmount;

    public final band.gosrock.domain.common.vo.QMoney feeVat;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final band.gosrock.domain.common.vo.QMoney interestFee;

    public final StringPath orderUuid = createString("orderUuid");

    public final DatePath<java.time.LocalDate> paidOutDate = createDate("paidOutDate", java.time.LocalDate.class);

    public final band.gosrock.domain.common.vo.QMoney paymentAmount;

    public final StringPath paymentKey = createString("paymentKey");

    public final EnumPath<band.gosrock.infrastructure.outer.api.tossPayments.dto.response.TossPaymentMethod> paymentMethod = createEnum("paymentMethod", band.gosrock.infrastructure.outer.api.tossPayments.dto.response.TossPaymentMethod.class);

    public final band.gosrock.domain.common.vo.QMoney settlementAmount;

    public final DatePath<java.time.LocalDate> soldDate = createDate("soldDate", java.time.LocalDate.class);

    public final StringPath transactionKey = createString("transactionKey");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QTransactionSettlement(String variable) {
        this(TransactionSettlement.class, forVariable(variable), INITS);
    }

    public QTransactionSettlement(Path<? extends TransactionSettlement> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTransactionSettlement(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTransactionSettlement(PathMetadata metadata, PathInits inits) {
        this(TransactionSettlement.class, metadata, inits);
    }

    public QTransactionSettlement(Class<? extends TransactionSettlement> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.feeSupplyAmount = inits.isInitialized("feeSupplyAmount") ? new band.gosrock.domain.common.vo.QMoney(forProperty("feeSupplyAmount")) : null;
        this.feeVat = inits.isInitialized("feeVat") ? new band.gosrock.domain.common.vo.QMoney(forProperty("feeVat")) : null;
        this.interestFee = inits.isInitialized("interestFee") ? new band.gosrock.domain.common.vo.QMoney(forProperty("interestFee")) : null;
        this.paymentAmount = inits.isInitialized("paymentAmount") ? new band.gosrock.domain.common.vo.QMoney(forProperty("paymentAmount")) : null;
        this.settlementAmount = inits.isInitialized("settlementAmount") ? new band.gosrock.domain.common.vo.QMoney(forProperty("settlementAmount")) : null;
    }

}

