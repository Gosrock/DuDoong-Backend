package band.gosrock.domain.domains.event.repository;

import band.gosrock.domain.common.util.SliceUtil;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;

import static band.gosrock.domain.domains.event.domain.EventStatus.CLOSED;
import static band.gosrock.domain.domains.event.domain.EventStatus.OPEN;
import static band.gosrock.domain.domains.event.domain.QEvent.event;

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
        List<Event> openEvents =
                queryFactory
                        .selectFrom(event)
                        .where(eqStatusOpen().and(nameContains(keyword)))
                        .orderBy(createdAtAsc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize() + 1)
                        .fetch();

        final long remainingSize = Math.max(pageable.getPageSize() - openEvents.size(), 0);
        if (remainingSize > 0) {
            openEvents.addAll(queryClosedEventsByKeywordAndSize(keyword, pageable, remainingSize));
        }
        return SliceUtil.valueOf(openEvents, pageable);
    }

    @Override
    public List<Event> queryEventsByEndAtBeforeAndStatusOpen(LocalDateTime time) {
        return queryFactory.selectFrom(event).where(endAtBefore(time), statusEq(OPEN)).fetch();
    }

    private List<Event> queryClosedEventsByKeywordAndSize(
            String keyword, Pageable pageable, Long size) {
        final long totalOpenEventsSize = queryCountByKeywordAndStatus(keyword, OPEN);
        final long closedEventsOffset = Math.max(pageable.getOffset() - totalOpenEventsSize, 0);
        return queryFactory
                .selectFrom(event)
                .where(eqStatusClosed().and(nameContains(keyword)))
                .orderBy(startAtDesc())
                .offset(closedEventsOffset)
                .limit(size + 1)
                .fetch();
    }

    private long queryCountByKeywordAndStatus(String keyword, EventStatus status) {
        return queryFactory
                .from(event)
                .where(statusEq(status).and(nameContains(keyword)))
                .fetchCount();
    }

    private BooleanExpression hostIdIn(List<Long> hostId) {
        return event.hostId.in(hostId);
    }

    private BooleanExpression eqStatusOpen() {
        return event.status.eq(OPEN);
    }

    private BooleanExpression eqStatusClosed() {
        return event.status.eq(CLOSED);
    }

    private BooleanExpression statusEq(EventStatus status) {
        return event.status.eq(status);
    }

    private BooleanExpression nameContains(String keyword) {
        return keyword == null ? null : event.eventBasic.name.containsIgnoreCase(keyword);
    }

    private OrderSpecifier<LocalDateTime> createdAtDesc() {
        return event.createdAt.desc();
    }

    private OrderSpecifier<LocalDateTime> createdAtAsc() {
        return event.createdAt.asc();
    }

    private OrderSpecifier<LocalDateTime> startAtAsc() {
        return event.eventBasic.startAt.asc();
    }

    private OrderSpecifier<LocalDateTime> startAtDesc() {
        return event.eventBasic.startAt.desc();
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
