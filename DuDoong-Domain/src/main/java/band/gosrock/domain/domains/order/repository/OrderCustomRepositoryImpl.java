package band.gosrock.domain.domains.order.repository;

import static band.gosrock.domain.domains.event.domain.QEvent.event;
import static band.gosrock.domain.domains.order.domain.QOrder.order;
import static band.gosrock.domain.domains.order.domain.QOrderLineItem.orderLineItem;
import static band.gosrock.domain.domains.user.domain.QUser.user;

import band.gosrock.domain.common.util.SliceUtil;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import band.gosrock.domain.domains.order.repository.condition.FindEventOrdersCondition;
import band.gosrock.domain.domains.order.repository.condition.FindMyPageOrderCondition;
import band.gosrock.domain.domains.user.domain.AccountState;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.support.PageableExecutionUtils;

@RequiredArgsConstructor
public class OrderCustomRepositoryImpl implements OrderCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Order> findByOrderUuid(String orderUuid) {
        Order find =
                queryFactory
                        .selectFrom(order)
                        .leftJoin(order.orderLineItems, orderLineItem)
                        .fetchJoin()
                        .where(order.uuid.eq(orderUuid))
                        .fetchOne();

        return Optional.ofNullable(find);
    }

    @Override
    public Slice<Order> findMyOrders(FindMyPageOrderCondition condition, Pageable pageable) {
        List<Order> orders =
                queryFactory
                        .selectFrom(order)
                        .join(event)
                        .on(order.eventId.eq(event.id))
                        .where(
                                eqUserId(condition.getUserId()),
                                openingState(condition.getShowing()),
                                order.orderStatus.notIn(
                                        OrderStatus.FAILED,
                                        OrderStatus.PENDING_PAYMENT,
                                        OrderStatus.READY,
                                        OrderStatus.OUTDATED))
                        .orderBy(order.id.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize() + 1)
                        .fetch();

        return SliceUtil.valueOf(orders, pageable);
    }

    @Override
    public Page<Order> findEventOrders(FindEventOrdersCondition condition, Pageable pageable) {
        List<Order> orders =
                queryFactory
                        .selectFrom(order)
                        .join(user)
                        .on(user.id.eq(order.userId))
                        .where(
                                // 주문 승인 요청 보여질 때만 지워진 유저를 보이게 하지 않습니다.
                                condition.showDeleteUserExpression(),
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
                                condition.showDeleteUserExpression(),
                                eqEventId(condition.getEventId()),
                                condition.getOrderStatusFilter(),
                                condition.getSearchStringFilter());

        return PageableExecutionUtils.getPage(orders, pageable, countQuery::fetchOne);
    }

    @Override
    public Optional<Order> findRecentOrder(Long userId) {
        Order findOrder =
                queryFactory
                        .selectFrom(order)
                        .where(
                                eqUserId(userId),
                                order.orderStatus.in(
                                        OrderStatus.PENDING_APPROVE,
                                        OrderStatus.APPROVED,
                                        OrderStatus.CONFIRM))
                        .orderBy(order.id.desc())
                        .fetchFirst();
        return Optional.ofNullable(findOrder);
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

    private BooleanExpression notDeletedUser() {
        return user.accountState.ne(AccountState.DELETED);
    }
}
