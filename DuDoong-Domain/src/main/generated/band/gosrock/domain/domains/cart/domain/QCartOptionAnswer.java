package band.gosrock.domain.domains.cart.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCartOptionAnswer is a Querydsl query type for CartOptionAnswer
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCartOptionAnswer extends EntityPathBase<CartOptionAnswer> {

    private static final long serialVersionUID = -597498975L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCartOptionAnswer cartOptionAnswer = new QCartOptionAnswer("cartOptionAnswer");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    public final band.gosrock.domain.common.vo.QMoney additionalPrice;

    public final StringPath answer = createString("answer");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> optionId = createNumber("optionId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QCartOptionAnswer(String variable) {
        this(CartOptionAnswer.class, forVariable(variable), INITS);
    }

    public QCartOptionAnswer(Path<? extends CartOptionAnswer> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCartOptionAnswer(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCartOptionAnswer(PathMetadata metadata, PathInits inits) {
        this(CartOptionAnswer.class, metadata, inits);
    }

    public QCartOptionAnswer(Class<? extends CartOptionAnswer> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.additionalPrice = inits.isInitialized("additionalPrice") ? new band.gosrock.domain.common.vo.QMoney(forProperty("additionalPrice")) : null;
    }

}

