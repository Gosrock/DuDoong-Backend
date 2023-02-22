package band.gosrock.domain.domains.event.repository;

import static band.gosrock.domain.domains.event.domain.EventStatus.OPEN;
import static band.gosrock.domain.domains.event.domain.QEvent.event;

import band.gosrock.domain.common.util.SliceUtil;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

@RequiredArgsConstructor
public class EventCustomRepositoryImpl implements EventCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Event> querySliceEventsByHostIdIn(List<Long> hostIds, Pageable pageable) {
        List<Event> events =
                queryFactory
                        .selectFrom(event)
                        .where(hostIdIn(hostIds))
                        .orderBy(event.id.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize() + 1)
                        .fetch();
        return SliceUtil.valueOf(events, pageable);
    }

    @Override
    public Slice<Event> querySliceEventsByStatus(EventStatus status, Pageable pageable) {
        List<Event> events =
                queryFactory
                        .selectFrom(event)
                        .where(eqStatusOpen())
                        .orderBy(event.id.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize() + 1)
                        .fetch();
        return SliceUtil.valueOf(events, pageable);
    }

    @Override
    public Slice<Event> querySliceEventsByKeyword(String keyword, Pageable pageable) {
        List<Event> events =
                queryFactory
                        .selectFrom(event)
                        .where(nameContains(keyword).and(eqStatusOpen()))
                        .orderBy(event.id.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize() + 1)
                        .fetch();
        return SliceUtil.valueOf(events, pageable);
    }

    private BooleanExpression hostIdIn(List<Long> hostId) {
        return event.hostId.in(hostId);
    }

    private BooleanExpression eqStatusOpen() {
        return event.status.eq(OPEN);
    }

    private BooleanExpression nameContains(String keyword) {
        return event.eventBasic.name.containsIgnoreCase(keyword);
    }
}
