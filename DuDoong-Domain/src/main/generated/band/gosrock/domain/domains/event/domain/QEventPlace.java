package band.gosrock.domain.domains.event.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEventPlace is a Querydsl query type for EventPlace
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QEventPlace extends BeanPath<EventPlace> {

    private static final long serialVersionUID = 1105975281L;

    public static final QEventPlace eventPlace = new QEventPlace("eventPlace");

    public final NumberPath<Double> latitude = createNumber("latitude", Double.class);

    public final NumberPath<Double> longitude = createNumber("longitude", Double.class);

    public final StringPath placeAddress = createString("placeAddress");

    public final StringPath placeName = createString("placeName");

    public final BooleanPath updated = createBoolean("updated");

    public QEventPlace(String variable) {
        super(EventPlace.class, forVariable(variable));
    }

    public QEventPlace(Path<? extends EventPlace> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEventPlace(PathMetadata metadata) {
        super(EventPlace.class, metadata);
    }

}

