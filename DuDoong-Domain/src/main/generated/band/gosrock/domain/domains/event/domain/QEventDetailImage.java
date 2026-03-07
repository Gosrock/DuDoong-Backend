package band.gosrock.domain.domains.event.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEventDetailImage is a Querydsl query type for EventDetailImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEventDetailImage extends EntityPathBase<EventDetailImage> {

    private static final long serialVersionUID = 652202740L;

    public static final QEventDetailImage eventDetailImage = new QEventDetailImage("eventDetailImage");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> eventId = createNumber("eventId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QEventDetailImage(String variable) {
        super(EventDetailImage.class, forVariable(variable));
    }

    public QEventDetailImage(Path<? extends EventDetailImage> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEventDetailImage(PathMetadata metadata) {
        super(EventDetailImage.class, metadata);
    }

}

