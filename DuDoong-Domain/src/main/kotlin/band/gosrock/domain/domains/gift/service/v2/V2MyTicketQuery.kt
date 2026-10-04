package band.gosrock.domain.domains.gift.service.v2

import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.QIssuedTicket.issuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.QIssuedTicketOptionAnswer.issuedTicketOptionAnswer
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.QOrder.order
import band.gosrock.domain.domains.order.service.v2.V2OrderStatus
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component

/** v2 티켓탭 조회 (#719, T-1·T-2). 티켓은 옵션 답변을 fetch join 해 유료 옵션 금액 합을 N+1 없이 계산한다 */
@Component
class V2MyTicketQuery(private val queryFactory: JPAQueryFactory) {

    /** 지금 내가 소유한 티켓 전체 (취소 포함, idx_issued_ticket_user_id_id) */
    fun findOwnedTickets(userId: Long): List<IssuedTicket> =
        queryFactory.selectFrom(issuedTicket).distinct()
            .leftJoin(issuedTicket.issuedTicketOptionAnswers, issuedTicketOptionAnswer).fetchJoin()
            .where(issuedTicket.userInfo.userId.eq(userId))
            .fetch()

    fun findTicketsByIds(ids: Collection<Long>): List<IssuedTicket> =
        if (ids.isEmpty()) emptyList()
        else queryFactory.selectFrom(issuedTicket).distinct()
            .leftJoin(issuedTicket.issuedTicketOptionAnswers, issuedTicketOptionAnswer).fetchJoin()
            .where(issuedTicket.id.`in`(ids))
            .fetch()

    /** 발급 티켓이 없는 내 주문 중 티켓탭에 보이는 것: 승인 대기·거절 (idx_order_user_id_id) */
    fun findWaitingOrRefusedOrders(userId: Long): List<Order> =
        queryFactory.selectFrom(order)
            .where(order.userId.eq(userId), V2OrderStatus.PENDING_APPROVE.predicate().or(V2OrderStatus.REFUSED.predicate()))
            .fetch()

    fun findOrdersByUuids(uuids: Collection<String>): List<Order> =
        if (uuids.isEmpty()) emptyList() else queryFactory.selectFrom(order).where(order.uuid.`in`(uuids)).fetch()
}
