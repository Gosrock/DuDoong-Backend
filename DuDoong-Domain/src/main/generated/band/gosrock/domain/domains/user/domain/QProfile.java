package band.gosrock.domain.domains.user.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProfile is a Querydsl query type for Profile
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QProfile extends BeanPath<Profile> {

    private static final long serialVersionUID = -1916784112L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProfile profile = new QProfile("profile");

    public final StringPath email = createString("email");

    public final StringPath name = createString("name");

    public final band.gosrock.domain.common.vo.QPhoneNumberVo phoneNumberVo;

    public final band.gosrock.domain.common.vo.QImageVo profileImage;

    public QProfile(String variable) {
        this(Profile.class, forVariable(variable), INITS);
    }

    public QProfile(Path<? extends Profile> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProfile(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProfile(PathMetadata metadata, PathInits inits) {
        this(Profile.class, metadata, inits);
    }

    public QProfile(Class<? extends Profile> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.phoneNumberVo = inits.isInitialized("phoneNumberVo") ? new band.gosrock.domain.common.vo.QPhoneNumberVo(forProperty("phoneNumberVo")) : null;
        this.profileImage = inits.isInitialized("profileImage") ? new band.gosrock.domain.common.vo.QImageVo(forProperty("profileImage")) : null;
    }

}

