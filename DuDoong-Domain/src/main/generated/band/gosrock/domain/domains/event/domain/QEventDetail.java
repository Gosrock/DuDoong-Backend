package band.gosrock.domain.domains.event.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEventDetail is a Querydsl query type for EventDetail
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QEventDetail extends BeanPath<EventDetail> {

    private static final long serialVersionUID = -423954777L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEventDetail eventDetail = new QEventDetail("eventDetail");

    public final StringPath content = createString("content");

    public final band.gosrock.domain.common.vo.QImageVo posterImage;

    public QEventDetail(String variable) {
        this(EventDetail.class, forVariable(variable), INITS);
    }

    public QEventDetail(Path<? extends EventDetail> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEventDetail(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEventDetail(PathMetadata metadata, PathInits inits) {
        this(EventDetail.class, metadata, inits);
    }

    public QEventDetail(Class<? extends EventDetail> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.posterImage = inits.isInitialized("posterImage") ? new band.gosrock.domain.common.vo.QImageVo(forProperty("posterImage")) : null;
    }

}

