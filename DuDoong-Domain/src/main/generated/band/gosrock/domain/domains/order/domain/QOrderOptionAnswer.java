package band.gosrock.domain.domains.order.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrderOptionAnswer is a Querydsl query type for OrderOptionAnswer
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrderOptionAnswer extends EntityPathBase<OrderOptionAnswer> {

    private static final long serialVersionUID = -1090088215L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrderOptionAnswer orderOptionAnswer = new QOrderOptionAnswer("orderOptionAnswer");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    public final band.gosrock.domain.common.vo.QMoney additionalPrice;

    public final StringPath answer = createString("answer");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> optionId = createNumber("optionId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QOrderOptionAnswer(String variable) {
        this(OrderOptionAnswer.class, forVariable(variable), INITS);
    }

    public QOrderOptionAnswer(Path<? extends OrderOptionAnswer> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrderOptionAnswer(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrderOptionAnswer(PathMetadata metadata, PathInits inits) {
        this(OrderOptionAnswer.class, metadata, inits);
    }

    public QOrderOptionAnswer(Class<? extends OrderOptionAnswer> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.additionalPrice = inits.isInitialized("additionalPrice") ? new band.gosrock.domain.common.vo.QMoney(forProperty("additionalPrice")) : null;
    }

}

