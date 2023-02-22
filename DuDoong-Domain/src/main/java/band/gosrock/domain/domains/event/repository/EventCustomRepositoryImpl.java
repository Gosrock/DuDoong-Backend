package band.gosrock.domain.domains.event.repository;

import static band.gosrock.domain.domains.event.domain.QEvent.event;

import band.gosrock.domain.common.util.SliceUtil;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

@RequiredArgsConstructor
public class EventCustomRepositoryImpl implements EventCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Event> querySliceEventsByHostIdIn(List<Long> hostId, Pageable pageable) {
        List<Event> events =
                queryFactory
                        .selectFrom(event)
                        .where(event.hostId.in(hostId))
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
                        .where(event.status.eq(status))
                        .orderBy(event.id.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize() + 1)
                        .fetch();
        return SliceUtil.valueOf(events, pageable);
    }
}
