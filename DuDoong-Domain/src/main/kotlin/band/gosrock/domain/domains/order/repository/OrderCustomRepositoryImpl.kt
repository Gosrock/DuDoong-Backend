package band.gosrock.domain.domains.order.repository

import band.gosrock.domain.common.util.SliceUtil
import band.gosrock.domain.domains.event.domain.QEvent.event
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.QOrder.order
import band.gosrock.domain.domains.order.domain.QOrderLineItem.orderLineItem
import band.gosrock.domain.domains.order.repository.condition.FindEventOrdersCondition
import band.gosrock.domain.domains.order.repository.condition.FindMyPageOrderCondition
import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.domain.QUser.user
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.DateTemplate
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDateTime
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.support.PageableExecutionUtils

class OrderCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : OrderCustomRepository {

    override fun findByOrderUuid(orderUuid: String): Optional<Order> {
        val find = queryFactory
            .selectFrom(order)
            .leftJoin(order.orderLineItems, orderLineItem)
            .fetchJoin()
            .where(order.uuid.eq(orderUuid))
            .fetchOne()
        return Optional.ofNullable(find)
    }

    override fun findMyOrders(condition: FindMyPageOrderCondition, pageable: Pageable): Slice<Order> {
        val orders = queryFactory
            .selectFrom(order)
            .join(event)
            .on(order.eventId.eq(event.id))
            .where(
                eqUserId(condition.userId),
                openingState(condition.showing),
                order.orderStatus.notIn(
                    OrderStatus.FAILED,
                    OrderStatus.PENDING_PAYMENT,
                    OrderStatus.READY,
                    OrderStatus.OUTDATED
                )
            )
            .orderBy(order.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize + 1L)
            .fetch()
        return SliceUtil.valueOf(orders, pageable)
    }

    override fun findEventOrders(condition: FindEventOrdersCondition, pageable: Pageable): Page<Order> {
        val orders = queryFactory
            .selectFrom(order)
            .join(user)
            .on(user.id.eq(order.userId))
            .where(
                condition.showDeleteUserExpression(),
                eqEventId(condition.eventId),
                condition.getOrderStatusFilter(),
                condition.getSearchStringFilter()
            )
            .orderBy(order.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery: JPAQuery<Long> = queryFactory
            .select(order.count())
            .from(order)
            .join(user)
            .on(user.id.eq(order.userId))
            .where(
                condition.showDeleteUserExpression(),
                eqEventId(condition.eventId),
                condition.getOrderStatusFilter(),
                condition.getSearchStringFilter()
            )

        return PageableExecutionUtils.getPage(orders, pageable) { countQuery.fetchOne()!! }
    }

    override fun findRecentOrder(userId: Long): Optional<Order> {
        val findOrder = queryFactory
            .selectFrom(order)
            .where(
                eqUserId(userId),
                order.orderStatus.`in`(
                    OrderStatus.PENDING_APPROVE,
                    OrderStatus.APPROVED,
                    OrderStatus.CONFIRM
                )
            )
            .orderBy(order.id.desc())
            .fetchFirst()
        return Optional.ofNullable(findOrder)
    }

    private fun eqUserId(userId: Long?): BooleanExpression? =
        if (userId == null) null else order.userId.eq(userId)

    private fun eqEventId(eventId: Long?): BooleanExpression? =
        if (eventId == null) null else order.eventId.eq(eventId)

    private fun openingState(isShowing: Boolean?): BooleanExpression {
        val eventEndAtTemplate: DateTemplate<LocalDateTime> = Expressions.dateTemplate(
            LocalDateTime::class.java,
            "TIMESTAMPADD(MINUTE,{0}, {1}) ",
            event.eventBasic.runTime,
            event.eventBasic.startAt
        )
        val now = LocalDateTime.now()
        return if (isShowing == true) eventEndAtTemplate.after(now) else eventEndAtTemplate.before(now)
    }
}
