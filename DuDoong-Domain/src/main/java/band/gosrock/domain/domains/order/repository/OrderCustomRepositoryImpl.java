package band.gosrock.domain.domains.order.repository;

import static band.gosrock.domain.domains.event.domain.QEvent.event;
import static band.gosrock.domain.domains.order.domain.QOrder.order;

import band.gosrock.domain.domains.order.domain.Order;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

@RequiredArgsConstructor
public class OrderCustomRepositoryImpl implements OrderCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Order> getOrdersWithPage(Long userId, Pageable pageable) {

        DateTemplate<LocalDateTime> localDateTimeDateTemplate =
                Expressions.dateTemplate(
                        LocalDateTime.class,
                        "TIMESTAMPADD(MINUTE,{0}, {1}) ",
                        event.eventBasic.runTime,
                        event.eventBasic.startAt);
        List<Order> issuedTickets =
                queryFactory
                        .selectFrom(order)
                        .join(event)
                        .on(order.eventId.eq(event.id))
                        .where(
                                userIdEq(userId),
                                localDateTimeDateTemplate.after(LocalDateTime.now()))
                        .orderBy(order.id.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();

        JPAQuery<Long> countQuery =
                queryFactory
                        .select(order.count())
                        .from(order)
                        .where(
                                userIdEq(userId),
                                localDateTimeDateTemplate.before(LocalDateTime.now()));

        return PageableExecutionUtils.getPage(issuedTickets, pageable, countQuery::fetchOne);
    }

    private BooleanExpression userIdEq(Long userId) {
        return userId == null ? null : order.userId.eq(userId);
    }
}
