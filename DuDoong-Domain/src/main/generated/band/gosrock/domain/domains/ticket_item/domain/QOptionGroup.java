package band.gosrock.domain.domains.ticket_item.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOptionGroup is a Querydsl query type for OptionGroup
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOptionGroup extends EntityPathBase<OptionGroup> {

    private static final long serialVersionUID = -2142291110L;

    public static final QOptionGroup optionGroup = new QOptionGroup("optionGroup");

    public final StringPath description = createString("description");

    public final NumberPath<Long> eventId = createNumber("eventId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isEssential = createBoolean("isEssential");

    public final StringPath name = createString("name");

    public final EnumPath<OptionGroupStatus> optionGroupStatus = createEnum("optionGroupStatus", OptionGroupStatus.class);

    public final ListPath<Option, QOption> options = this.<Option, QOption>createList("options", Option.class, QOption.class, PathInits.DIRECT2);

    public final EnumPath<OptionGroupType> type = createEnum("type", OptionGroupType.class);

    public QOptionGroup(String variable) {
        super(OptionGroup.class, forVariable(variable));
    }

    public QOptionGroup(Path<? extends OptionGroup> path) {
        super(path.getType(), path.getMetadata());
    }

    public QOptionGroup(PathMetadata metadata) {
        super(OptionGroup.class, metadata);
    }

}

