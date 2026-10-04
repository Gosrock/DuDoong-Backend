package band.gosrock.domain.domains.order.service.v2

import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import band.gosrock.domain.domains.issuedTicket.domain.QIssuedTicket.issuedTicket
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderPaymentChannel
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.QOrder.order
import band.gosrock.domain.domains.order.domain.QOrderLineItem.orderLineItem
import band.gosrock.domain.domains.order.domain.QOrderOptionAnswer.orderOptionAnswer
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Component

/**
 * 같은 사용자의 최근 주문 (중복 요청 판정용 스칼라).
 * @property lines 라인별 (수량, (옵션 행 id, 답변) 목록) — 라인 id 순, 답변은 옵션 행 id 순
 */
data class V2RecentOrder(
    val orderUuid: String,
    val orderStatus: OrderStatus,
    val paymentChannel: OrderPaymentChannel?,
    val depositorName: String?,
    val lines: List<Pair<Long, List<Pair<Long?, String?>>>>,
)

/**
 * v2 사용자 앱 주문 조회 (#718). v2 에서만 쓴다.
 * 쓰기 전 판정(중복 요청·취소 막는 티켓)은 엔티티를 영속성 컨텍스트에 올리지 않는 스칼라 조회로 한다 (open-in-view, README)
 */
@Component
class V2UserOrderQuery(private val queryFactory: JPAQueryFactory) {

    /** 내 주문 목록 (최신 순). [status] null 이면 보이는 주문 전체 (idx_order_user_id_id) */
    fun findMyOrders(userId: Long, status: V2MyOrderStatus?, pageable: Pageable): Page<Order> {
        val where = arrayOf(order.userId.eq(userId), status?.predicate() ?: V2MyOrderStatus.visiblePredicate())
        val content = queryFactory.selectFrom(order)
            .where(*where)
            .orderBy(order.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
        val countQuery = queryFactory.select(order.count()).from(order).where(*where)
        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    /** [since] 이후 만든 이 사용자의 이 티켓 주문 중 [statuses] 상태인 것 (최신 순). 라인·답변까지 스칼라로 읽는다 */
    fun findRecentOrders(userId: Long, ticketItemId: Long, since: LocalDateTime, statuses: Collection<OrderStatus>): List<V2RecentOrder> {
        val rows = queryFactory
            .select(
                order.id, order.uuid, order.orderStatus, order.paymentChannel, order.depositorName,
                orderLineItem.id, orderLineItem.quantity, orderOptionAnswer.optionId, orderOptionAnswer.answer,
            )
            .from(order)
            .join(order.orderLineItems, orderLineItem)
            .leftJoin(orderLineItem.orderOptionAnswers, orderOptionAnswer)
            .where(
                order.userId.eq(userId),
                order.createdAt.goe(since),
                order.orderStatus.`in`(statuses),
                // 1주문 1종류라 라인을 티켓으로 걸러도 주문의 라인이 전부 남는다
                orderLineItem.orderItem.itemId.eq(ticketItemId),
            )
            .fetch()
        return rows.groupBy { it.get(order.id) }.values
            .sortedByDescending { it.first().get(order.id) }
            .map { orderRows ->
                val head = orderRows.first()
                V2RecentOrder(
                    orderUuid = head.get(order.uuid)!!,
                    orderStatus = head.get(order.orderStatus)!!,
                    paymentChannel = head.get(order.paymentChannel),
                    depositorName = head.get(order.depositorName),
                    lines = orderRows.groupBy { it.get(orderLineItem.id) }.toSortedMap(compareBy { it }).values.map { lineRows ->
                        val qty = lineRows.first().get(orderLineItem.quantity) ?: 0L
                        qty to lineRows.filter { it.get(orderOptionAnswer.optionId) != null }
                            .map { it.get(orderOptionAnswer.optionId) to it.get(orderOptionAnswer.answer) }
                            .sortedBy { it.first }
                    },
                )
            }
    }

    /**
     * 무료 확정 전(PENDING_PAYMENT)인 이 사용자의 이 티켓 v2 주문 수량 합 ([since] 이후 생성분, payment_channel 있음 = v2).
     * 1인 제한 검사에 더한다: 무료 선착순은 생성과 확정(발급)이 다른 트랜잭션이라, 확정 전 주문이 발급 수에 아직 안 잡힌다
     */
    fun sumUnconfirmedV2Quantity(userId: Long, ticketItemId: Long, since: LocalDateTime): Long =
        queryFactory.select(orderLineItem.quantity.sum()).from(order)
            .join(order.orderLineItems, orderLineItem)
            .where(
                order.userId.eq(userId),
                orderLineItem.orderItem.itemId.eq(ticketItemId),
                order.orderStatus.eq(OrderStatus.PENDING_PAYMENT),
                order.paymentChannel.isNotNull,
                order.createdAt.goe(since),
            )
            .fetchOne() ?: 0L

    /**
     * 사용자 취소를 막는 발급 티켓 수: 입장한 티켓 + 주문자 소유가 아닌 티켓(8단계 선물로 넘어간 티켓 대비). 취소된 티켓은 제외
     */
    fun countCancelBlockingTickets(orderUuid: String, ownerUserId: Long): Long =
        queryFactory.select(issuedTicket.count()).from(issuedTicket)
            .where(
                issuedTicket.orderUuid.eq(orderUuid),
                issuedTicket.issuedTicketStatus.ne(IssuedTicketStatus.CANCELED),
                issuedTicket.issuedTicketStatus.eq(IssuedTicketStatus.ENTRANCE_COMPLETED)
                    .or(issuedTicket.userInfo.userId.ne(ownerUserId)),
            )
            .fetchOne() ?: 0L
}
