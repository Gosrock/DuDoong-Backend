package band.gosrock.domain.domains.event.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEventBasic is a Querydsl query type for EventBasic
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QEventBasic extends BeanPath<EventBasic> {

    private static final long serialVersionUID = 1092735768L;

    public static final QEventBasic eventBasic = new QEventBasic("eventBasic");

    public final StringPath name = createString("name");

    public final NumberPath<Long> runTime = createNumber("runTime", Long.class);

    public final DateTimePath<java.time.LocalDateTime> startAt = createDateTime("startAt", java.time.LocalDateTime.class);

    public QEventBasic(String variable) {
        super(EventBasic.class, forVariable(variable));
    }

    public QEventBasic(Path<? extends EventBasic> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEventBasic(PathMetadata metadata) {
        super(EventBasic.class, metadata);
    }

}

