package band.gosrock.domain.domains.issuedTicket.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QIssuedTicketItemInfoVo is a Querydsl query type for IssuedTicketItemInfoVo
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QIssuedTicketItemInfoVo extends BeanPath<IssuedTicketItemInfoVo> {

    private static final long serialVersionUID = 1901306230L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QIssuedTicketItemInfoVo issuedTicketItemInfoVo = new QIssuedTicketItemInfoVo("issuedTicketItemInfoVo");

    public final EnumPath<band.gosrock.domain.domains.ticket_item.domain.TicketPayType> payType = createEnum("payType", band.gosrock.domain.domains.ticket_item.domain.TicketPayType.class);

    public final band.gosrock.domain.common.vo.QMoney price;

    public final NumberPath<Long> ticketItemId = createNumber("ticketItemId", Long.class);

    public final StringPath ticketName = createString("ticketName");

    public final EnumPath<band.gosrock.domain.domains.ticket_item.domain.TicketType> ticketType = createEnum("ticketType", band.gosrock.domain.domains.ticket_item.domain.TicketType.class);

    public QIssuedTicketItemInfoVo(String variable) {
        this(IssuedTicketItemInfoVo.class, forVariable(variable), INITS);
    }

    public QIssuedTicketItemInfoVo(Path<? extends IssuedTicketItemInfoVo> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QIssuedTicketItemInfoVo(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QIssuedTicketItemInfoVo(PathMetadata metadata, PathInits inits) {
        this(IssuedTicketItemInfoVo.class, metadata, inits);
    }

    public QIssuedTicketItemInfoVo(Class<? extends IssuedTicketItemInfoVo> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.price = inits.isInitialized("price") ? new band.gosrock.domain.common.vo.QMoney(forProperty("price")) : null;
    }

}

