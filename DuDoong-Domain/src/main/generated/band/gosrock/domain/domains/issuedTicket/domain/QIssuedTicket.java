package band.gosrock.domain.domains.issuedTicket.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QIssuedTicket is a Querydsl query type for IssuedTicket
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QIssuedTicket extends EntityPathBase<IssuedTicket> {

    private static final long serialVersionUID = -836452516L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QIssuedTicket issuedTicket = new QIssuedTicket("issuedTicket");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> enteredAt = createDateTime("enteredAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> eventId = createNumber("eventId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath issuedTicketNo = createString("issuedTicketNo");

    public final ListPath<IssuedTicketOptionAnswer, QIssuedTicketOptionAnswer> issuedTicketOptionAnswers = this.<IssuedTicketOptionAnswer, QIssuedTicketOptionAnswer>createList("issuedTicketOptionAnswers", IssuedTicketOptionAnswer.class, QIssuedTicketOptionAnswer.class, PathInits.DIRECT2);

    public final EnumPath<IssuedTicketStatus> issuedTicketStatus = createEnum("issuedTicketStatus", IssuedTicketStatus.class);

    public final QIssuedTicketItemInfoVo itemInfo;

    public final NumberPath<Long> orderLineId = createNumber("orderLineId", Long.class);

    public final StringPath orderUuid = createString("orderUuid");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final QIssuedTicketUserInfoVo userInfo;

    public final StringPath uuid = createString("uuid");

    public QIssuedTicket(String variable) {
        this(IssuedTicket.class, forVariable(variable), INITS);
    }

    public QIssuedTicket(Path<? extends IssuedTicket> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QIssuedTicket(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QIssuedTicket(PathMetadata metadata, PathInits inits) {
        this(IssuedTicket.class, metadata, inits);
    }

    public QIssuedTicket(Class<? extends IssuedTicket> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.itemInfo = inits.isInitialized("itemInfo") ? new QIssuedTicketItemInfoVo(forProperty("itemInfo"), inits.get("itemInfo")) : null;
        this.userInfo = inits.isInitialized("userInfo") ? new QIssuedTicketUserInfoVo(forProperty("userInfo"), inits.get("userInfo")) : null;
    }

}

