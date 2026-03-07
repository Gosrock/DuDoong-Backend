package band.gosrock.domain.domains.host.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QHostProfile is a Querydsl query type for HostProfile
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QHostProfile extends BeanPath<HostProfile> {

    private static final long serialVersionUID = 1813807819L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QHostProfile hostProfile = new QHostProfile("hostProfile");

    public final StringPath contactEmail = createString("contactEmail");

    public final StringPath contactNumber = createString("contactNumber");

    public final StringPath introduce = createString("introduce");

    public final StringPath name = createString("name");

    public final band.gosrock.domain.common.vo.QImageVo profileImage;

    public QHostProfile(String variable) {
        this(HostProfile.class, forVariable(variable), INITS);
    }

    public QHostProfile(Path<? extends HostProfile> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QHostProfile(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QHostProfile(PathMetadata metadata, PathInits inits) {
        this(HostProfile.class, metadata, inits);
    }

    public QHostProfile(Class<? extends HostProfile> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.profileImage = inits.isInitialized("profileImage") ? new band.gosrock.domain.common.vo.QImageVo(forProperty("profileImage")) : null;
    }

}

