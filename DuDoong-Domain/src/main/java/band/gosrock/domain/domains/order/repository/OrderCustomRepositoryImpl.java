package band.gosrock.domain.domains.order.repository;

import static band.gosrock.domain.domains.event.domain.QEvent.event;
import static band.gosrock.domain.domains.order.domain.QOrder.order;
import static band.gosrock.domain.domains.user.domain.QUser.user;

import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.repository.condition.FindEventOrdersCondition;
import band.gosrock.domain.domains.order.repository.condition.FindMyPageOrderCondition;
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
    public Page<Order> findMyOrders(FindMyPageOrderCondition condition, Pageable pageable) {

        List<Order> orders =
                queryFactory
                        .selectFrom(order)
                        .join(event)
                        .on(order.eventId.eq(event.id))
                        .where(
                                eqUserId(condition.getUserId()),
                                openingState(condition.getShowing()))
                        .orderBy(order.id.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();

        JPAQuery<Long> countQuery =
                queryFactory
                        .select(order.count())
                        .join(event)
                        .on(order.eventId.eq(event.id))
                        .from(order)
                        .where(
                                eqUserId(condition.getUserId()),
                                openingState(condition.getShowing()));

        return PageableExecutionUtils.getPage(orders, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<Order> findEventOrders(FindEventOrdersCondition condition, Pageable pageable) {
        List<Order> orders =
                queryFactory
                        .selectFrom(order)
                        .where(
                                eqEventId(condition.getEventId()),
                                condition.getOrderStatusFilter(),
                                condition.getSearchStringFilter())
                        .orderBy(order.id.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();

        JPAQuery<Long> countQuery =
                queryFactory
                        .select(order.count())
                        .from(order)
                        .join(user)
                        .on(user.id.eq(order.userId))
                        .where(
                                eqEventId(condition.getEventId()),
                                condition.getOrderStatusFilter(),
                                condition.getSearchStringFilter());

        return PageableExecutionUtils.getPage(orders, pageable, countQuery::fetchOne);
    }

    private BooleanExpression eqUserId(Long userId) {
        return userId == null ? null : order.userId.eq(userId);
    }

    private BooleanExpression eqEventId(Long eventId) {
        return eventId == null ? null : order.eventId.eq(eventId);
    }

    private BooleanExpression openingState(Boolean isShowing) {
        DateTemplate<LocalDateTime> eventEndAtTemplate =
                Expressions.dateTemplate(
                        LocalDateTime.class,
                        "TIMESTAMPADD(MINUTE,{0}, {1}) ",
                        event.eventBasic.runTime,
                        event.eventBasic.startAt);
        LocalDateTime now = LocalDateTime.now();
        return isShowing == Boolean.TRUE
                ? eventEndAtTemplate.after(now)
                : eventEndAtTemplate.before(now);
    }
}
