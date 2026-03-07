package band.gosrock.domain.common.vo;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDateTimePeriod is a Querydsl query type for DateTimePeriod
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QDateTimePeriod extends BeanPath<DateTimePeriod> {

    private static final long serialVersionUID = 1377356705L;

    public static final QDateTimePeriod dateTimePeriod = new QDateTimePeriod("dateTimePeriod");

    public final DateTimePath<java.time.LocalDateTime> endAt = createDateTime("endAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> startAt = createDateTime("startAt", java.time.LocalDateTime.class);

    public QDateTimePeriod(String variable) {
        super(DateTimePeriod.class, forVariable(variable));
    }

    public QDateTimePeriod(Path<? extends DateTimePeriod> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDateTimePeriod(PathMetadata metadata) {
        super(DateTimePeriod.class, metadata);
    }

}

