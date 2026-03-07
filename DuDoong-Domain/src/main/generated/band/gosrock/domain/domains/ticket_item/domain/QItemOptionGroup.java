package band.gosrock.domain.domains.ticket_item.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QItemOptionGroup is a Querydsl query type for ItemOptionGroup
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QItemOptionGroup extends EntityPathBase<ItemOptionGroup> {

    private static final long serialVersionUID = 1035281991L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QItemOptionGroup itemOptionGroup = new QItemOptionGroup("itemOptionGroup");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QTicketItem item;

    public final QOptionGroup optionGroup;

    public QItemOptionGroup(String variable) {
        this(ItemOptionGroup.class, forVariable(variable), INITS);
    }

    public QItemOptionGroup(Path<? extends ItemOptionGroup> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QItemOptionGroup(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QItemOptionGroup(PathMetadata metadata, PathInits inits) {
        this(ItemOptionGroup.class, metadata, inits);
    }

    public QItemOptionGroup(Class<? extends ItemOptionGroup> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.item = inits.isInitialized("item") ? new QTicketItem(forProperty("item"), inits.get("item")) : null;
        this.optionGroup = inits.isInitialized("optionGroup") ? new QOptionGroup(forProperty("optionGroup")) : null;
    }

}

