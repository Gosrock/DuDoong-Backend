package band.gosrock.domain.domains.event.repository;

import static band.gosrock.domain.domains.event.domain.EventStatus.CLOSED;
import static band.gosrock.domain.domains.event.domain.EventStatus.OPEN;
import static band.gosrock.domain.domains.event.domain.QEvent.event;

import band.gosrock.domain.common.util.SliceUtil;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
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
                        .orderBy(statusDesc(), createdAtDesc())
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
                        .where(statusEq(status))
                        .orderBy(statusDesc(), startAtAsc())
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
                        .where(eqStatusCanExposed(), nameContains(keyword))
                        .orderBy(statusDesc(), startAtAsc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize() + 1)
                        .fetch();
        return SliceUtil.valueOf(events, pageable);
    }

    @Override
    public List<Event> queryEventsByEndAtBeforeAndStatusOpen(LocalDateTime time) {
        return queryFactory.selectFrom(event).where(endAtBefore(time), statusEq(OPEN)).fetch();
    }

    private BooleanExpression hostIdIn(List<Long> hostId) {
        return event.hostId.in(hostId);
    }

    private BooleanExpression eqStatusOpen() {
        return event.status.eq(OPEN);
    }

    private BooleanExpression eqStatusCanExposed() {
        return event.status.eq(OPEN).or(event.status.eq(CLOSED));
    }

    private BooleanExpression statusEq(EventStatus status) {
        return event.status.eq(status);
    }

    private BooleanExpression statusNotEq(EventStatus status) {
        return event.status.eq(status).not();
    }

    private BooleanExpression nameContains(String keyword) {
        return keyword == null ? null : event.eventBasic.name.containsIgnoreCase(keyword);
    }

    private OrderSpecifier<LocalDateTime> createdAtDesc() {
        return event.createdAt.desc();
    }

    private OrderSpecifier<LocalDateTime> startAtAsc() {
        return event.eventBasic.startAt.asc();
    }

    private OrderSpecifier<EventStatus> statusDesc() {
        return event.status.desc();
    }

    private BooleanExpression endAtBefore(LocalDateTime time) {
        DateTemplate<LocalDateTime> eventEndAtTemplate =
                Expressions.dateTemplate(
                        LocalDateTime.class,
                        "TIMESTAMPADD(MINUTE,{0}, {1}) ",
                        event.eventBasic.runTime,
                        event.eventBasic.startAt);
        return eventEndAtTemplate.before(time);
    }
}
