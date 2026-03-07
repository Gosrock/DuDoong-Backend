package band.gosrock.domain.domains.ticket_item.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTicketItem is a Querydsl query type for TicketItem
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTicketItem extends EntityPathBase<TicketItem> {

    private static final long serialVersionUID = -1015648785L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTicketItem ticketItem = new QTicketItem("ticketItem");

    public final band.gosrock.domain.common.model.QBaseTimeEntity _super = new band.gosrock.domain.common.model.QBaseTimeEntity(this);

    public final band.gosrock.domain.common.vo.QAccountInfoVo accountInfo;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final NumberPath<Long> eventId = createNumber("eventId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isQuantityPublic = createBoolean("isQuantityPublic");

    public final BooleanPath isSellable = createBoolean("isSellable");

    public final ListPath<ItemOptionGroup, QItemOptionGroup> itemOptionGroups = this.<ItemOptionGroup, QItemOptionGroup>createList("itemOptionGroups", ItemOptionGroup.class, QItemOptionGroup.class, PathInits.DIRECT2);

    public final StringPath name = createString("name");

    public final EnumPath<TicketPayType> payType = createEnum("payType", TicketPayType.class);

    public final band.gosrock.domain.common.vo.QMoney price;

    public final NumberPath<Long> purchaseLimit = createNumber("purchaseLimit", Long.class);

    public final NumberPath<Long> quantity = createNumber("quantity", Long.class);

    public final DateTimePath<java.time.LocalDateTime> saleEndAt = createDateTime("saleEndAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> saleStartAt = createDateTime("saleStartAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> supplyCount = createNumber("supplyCount", Long.class);

    public final EnumPath<TicketItemStatus> ticketItemStatus = createEnum("ticketItemStatus", TicketItemStatus.class);

    public final EnumPath<TicketType> type = createEnum("type", TicketType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QTicketItem(String variable) {
        this(TicketItem.class, forVariable(variable), INITS);
    }

    public QTicketItem(Path<? extends TicketItem> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTicketItem(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTicketItem(PathMetadata metadata, PathInits inits) {
        this(TicketItem.class, metadata, inits);
    }

    public QTicketItem(Class<? extends TicketItem> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.accountInfo = inits.isInitialized("accountInfo") ? new band.gosrock.domain.common.vo.QAccountInfoVo(forProperty("accountInfo")) : null;
        this.price = inits.isInitialized("price") ? new band.gosrock.domain.common.vo.QMoney(forProperty("price")) : null;
    }

}

