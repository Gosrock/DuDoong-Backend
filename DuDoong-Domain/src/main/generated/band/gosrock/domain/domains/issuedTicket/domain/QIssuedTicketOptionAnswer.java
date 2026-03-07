package band.gosrock.domain.domains.issuedTicket.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QIssuedTicketOptionAnswer is a Querydsl query type for IssuedTicketOptionAnswer
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QIssuedTicketOptionAnswer extends EntityPathBase<IssuedTicketOptionAnswer> {

    private static final long serialVersionUID = 1896657423L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QIssuedTicketOptionAnswer issuedTicketOptionAnswer = new QIssuedTicketOptionAnswer("issuedTicketOptionAnswer");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    public final band.gosrock.domain.common.vo.QMoney additionalPrice;

    public final StringPath answer = createString("answer");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> optionId = createNumber("optionId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QIssuedTicketOptionAnswer(String variable) {
        this(IssuedTicketOptionAnswer.class, forVariable(variable), INITS);
    }

    public QIssuedTicketOptionAnswer(Path<? extends IssuedTicketOptionAnswer> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QIssuedTicketOptionAnswer(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QIssuedTicketOptionAnswer(PathMetadata metadata, PathInits inits) {
        this(IssuedTicketOptionAnswer.class, metadata, inits);
    }

    public QIssuedTicketOptionAnswer(Class<? extends IssuedTicketOptionAnswer> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.additionalPrice = inits.isInitialized("additionalPrice") ? new band.gosrock.domain.common.vo.QMoney(forProperty("additionalPrice")) : null;
    }

}

