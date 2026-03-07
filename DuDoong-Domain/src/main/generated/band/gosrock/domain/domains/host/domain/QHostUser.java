package band.gosrock.domain.domains.host.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QHostUser is a Querydsl query type for HostUser
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QHostUser extends EntityPathBase<HostUser> {

    private static final long serialVersionUID = -1148824087L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QHostUser hostUser = new QHostUser("hostUser");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    public final BooleanPath active = createBoolean("active");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final QHost host;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<HostRole> role = createEnum("role", HostRole.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QHostUser(String variable) {
        this(HostUser.class, forVariable(variable), INITS);
    }

    public QHostUser(Path<? extends HostUser> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QHostUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QHostUser(PathMetadata metadata, PathInits inits) {
        this(HostUser.class, metadata, inits);
    }

    public QHostUser(Class<? extends HostUser> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.host = inits.isInitialized("host") ? new QHost(forProperty("host"), inits.get("host")) : null;
    }

}

