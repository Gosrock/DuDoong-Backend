package band.gosrock.domain.domains.event.repository;

import static band.gosrock.domain.domains.event.domain.QEvent.event;

import band.gosrock.domain.common.util.QueryDslUtil;
import band.gosrock.domain.domains.event.domain.Event;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@RequiredArgsConstructor
public class EventCustomRepositoryImpl implements EventCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Event> querySliceEventByHostIdIn(
            List<Long> hostId, Long lastId, Pageable pageable) {
        OrderSpecifier[] orders = QueryDslUtil.getOrderSpecifiers(Event.class, pageable);
        List<Event> events =
                queryFactory
                        .selectFrom(event)
                        .where(event.hostId.in(hostId), lastIdLessThanEqual(lastId))
                        .orderBy(orders)
                        .limit(pageable.getPageSize() + 1)
                        .fetch();

        return checkLastPage(events, pageable);
    }

    private BooleanExpression lastIdLessThanEqual(Long lastId) {
        return lastId == null ? null : event.id.loe(lastId);
    }

    private Slice<Event> checkLastPage(List<Event> events, Pageable pageable) {
        boolean hasNext = false;
        if (events.size() > pageable.getPageSize()) {
            hasNext = true;
            events.remove(pageable.getPageSize());
        }
        return new SliceImpl<>(events, pageable, hasNext);
    }
}
