package band.gosrock.domain.domains.example.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QExampleEntity is a Querydsl query type for ExampleEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QExampleEntity extends EntityPathBase<ExampleEntity> {

    private static final long serialVersionUID = 77690041L;

    public static final QExampleEntity exampleEntity = new QExampleEntity("exampleEntity");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QExampleEntity(String variable) {
        super(ExampleEntity.class, forVariable(variable));
    }

    public QExampleEntity(Path<? extends ExampleEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QExampleEntity(PathMetadata metadata) {
        super(ExampleEntity.class, metadata);
    }

}

