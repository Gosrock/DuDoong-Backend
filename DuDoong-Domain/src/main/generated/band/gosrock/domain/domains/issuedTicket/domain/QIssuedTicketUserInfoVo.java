package band.gosrock.domain.domains.issuedTicket.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QIssuedTicketUserInfoVo is a Querydsl query type for IssuedTicketUserInfoVo
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QIssuedTicketUserInfoVo extends BeanPath<IssuedTicketUserInfoVo> {

    private static final long serialVersionUID = 1493149742L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QIssuedTicketUserInfoVo issuedTicketUserInfoVo = new QIssuedTicketUserInfoVo("issuedTicketUserInfoVo");

    public final StringPath email = createString("email");

    public final band.gosrock.domain.common.vo.QPhoneNumberVo phoneNumber;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final StringPath userName = createString("userName");

    public QIssuedTicketUserInfoVo(String variable) {
        this(IssuedTicketUserInfoVo.class, forVariable(variable), INITS);
    }

    public QIssuedTicketUserInfoVo(Path<? extends IssuedTicketUserInfoVo> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QIssuedTicketUserInfoVo(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QIssuedTicketUserInfoVo(PathMetadata metadata, PathInits inits) {
        this(IssuedTicketUserInfoVo.class, metadata, inits);
    }

    public QIssuedTicketUserInfoVo(Class<? extends IssuedTicketUserInfoVo> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.phoneNumber = inits.isInitialized("phoneNumber") ? new band.gosrock.domain.common.vo.QPhoneNumberVo(forProperty("phoneNumber")) : null;
    }

}

