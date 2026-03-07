package band.gosrock.domain.domains.settlement.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEventSettlement is a Querydsl query type for EventSettlement
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEventSettlement extends EntityPathBase<EventSettlement> {

    private static final long serialVersionUID = -832614612L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEventSettlement eventSettlement = new QEventSettlement("eventSettlement");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    public final band.gosrock.domain.common.vo.QMoney couponAmount;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final band.gosrock.domain.common.vo.QMoney dudoongAmount;

    public final band.gosrock.domain.common.vo.QMoney dudoongFee;

    public final NumberPath<Long> eventId = createNumber("eventId", Long.class);

    public final StringPath eventOrderExcelKey = createString("eventOrderExcelKey");

    public final EnumPath<EventSettlementStatus> eventSettlementStatus = createEnum("eventSettlementStatus", EventSettlementStatus.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final band.gosrock.domain.common.vo.QMoney paymentAmount;

    public final band.gosrock.domain.common.vo.QMoney pgFee;

    public final band.gosrock.domain.common.vo.QMoney pgFeeVat;

    public final band.gosrock.domain.common.vo.QMoney totalAmount;

    public final band.gosrock.domain.common.vo.QMoney totalSalesAmount;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QEventSettlement(String variable) {
        this(EventSettlement.class, forVariable(variable), INITS);
    }

    public QEventSettlement(Path<? extends EventSettlement> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEventSettlement(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEventSettlement(PathMetadata metadata, PathInits inits) {
        this(EventSettlement.class, metadata, inits);
    }

    public QEventSettlement(Class<? extends EventSettlement> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.couponAmount = inits.isInitialized("couponAmount") ? new band.gosrock.domain.common.vo.QMoney(forProperty("couponAmount")) : null;
        this.dudoongAmount = inits.isInitialized("dudoongAmount") ? new band.gosrock.domain.common.vo.QMoney(forProperty("dudoongAmount")) : null;
        this.dudoongFee = inits.isInitialized("dudoongFee") ? new band.gosrock.domain.common.vo.QMoney(forProperty("dudoongFee")) : null;
        this.paymentAmount = inits.isInitialized("paymentAmount") ? new band.gosrock.domain.common.vo.QMoney(forProperty("paymentAmount")) : null;
        this.pgFee = inits.isInitialized("pgFee") ? new band.gosrock.domain.common.vo.QMoney(forProperty("pgFee")) : null;
        this.pgFeeVat = inits.isInitialized("pgFeeVat") ? new band.gosrock.domain.common.vo.QMoney(forProperty("pgFeeVat")) : null;
        this.totalAmount = inits.isInitialized("totalAmount") ? new band.gosrock.domain.common.vo.QMoney(forProperty("totalAmount")) : null;
        this.totalSalesAmount = inits.isInitialized("totalSalesAmount") ? new band.gosrock.domain.common.vo.QMoney(forProperty("totalSalesAmount")) : null;
    }

}

