package band.gosrock.domain.domains.order.service.v2

import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.QOrder.order
import band.gosrock.domain.domains.order.domain.QOrderLineItem.orderLineItem
import band.gosrock.domain.domains.order.domain.QOrderOptionAnswer.orderOptionAnswer
import band.gosrock.domain.domains.order.exception.ExportTooManyOrdersException
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.user.domain.QUser.user
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import java.math.BigDecimal
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Component

/**
 * v2 호스트 주문 목록 검색 조건 (#712).
 * @property status null 이면 전체 ([V2OrderStatus.visiblePredicate])
 * @property searchType 검색어가 있을 때 기준. null 이면 이름 ([V2OrderSearchType])
 */
data class V2OrderSearch(
    val eventId: Long,
    val status: V2OrderStatus? = null,
    val searchType: V2OrderSearchType? = null,
    val keyword: String? = null,
)

/**
 * v2 호스트 주문 검색 기준 (#726). 이름·연락처는 v1 어드민 목록([band.gosrock.domain.domains.order.repository.condition.AdminTableSearchType])과 같은 기준
 * (현재 회원 정보, 부분 일치). 입금자명은 v2 주문에 저장된 값(`tbl_order.depositor_name`, DEC-022 #6) 부분 일치라 v1 주문은 걸리지 않는다.
 * v1 검색 enum 은 v1·v2 발급 티켓 목록이 같이 쓰므로 건드리지 않고 v2 주문 목록 전용으로 둔다
 */
enum class V2OrderSearchType(val needsUserJoin: Boolean, private val expression: (String) -> BooleanExpression) {
    PHONE(true, { keyword -> user.profile.phoneNumberVo.phoneNumber.contains(keyword) }),
    NAME(true, { keyword -> user.profile.name.contains(keyword) }),
    DEPOSITOR_NAME(false, { keyword -> order.depositorName.contains(keyword) });

    fun contains(keyword: String): BooleanExpression = expression(keyword)
}

/** 상태별 주문 건수. 검색어는 반영하고 상태 필터는 무시한다 (탭 건수) */
data class V2OrderCounts(
    val all: Long,
    val pendingApprove: Long,
    val approved: Long,
    val refused: Long,
    val canceled: Long,
    val failed: Long,
)

/** v2 호스트 주문 조회 쿼리 (목록·건수·엑셀·대시보드). v2 에서만 쓴다 */
@Component
class V2OrderQuery(private val queryFactory: JPAQueryFactory) {

    companion object {
        /** v2 엑셀 행 상한 (주문·발급 티켓 공통) */
        const val EXPORT_MAX_ROWS = 10_000
    }

    fun findPage(search: V2OrderSearch, pageable: Pageable): Page<Order> {
        val content = base(queryFactory.selectFrom(order), search, withStatus = true)
            .orderBy(order.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
        val countQuery = base(queryFactory.select(order.count()).from(order), search, withStatus = true)
        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    /**
     * 엑셀용 전체 (최신 순, 주문 라인 fetch join, 라인 옵션 답변은 별도 쿼리 1개로 미리 적재). [maxRows] 를 넘으면 조회하지 않고 400 (Order_400_19)
     */
    fun findAllForExport(search: V2OrderSearch, maxRows: Int): List<Order> {
        val total = base(queryFactory.select(order.count()).from(order), search, withStatus = true).fetchOne() ?: 0L
        if (total > maxRows) throw ExportTooManyOrdersException.EXCEPTION
        // 라인 옵션 답변(EAGER 컬렉션)을 먼저 쿼리 1개로 읽어 영속성 컨텍스트에 올린다 (#730).
        // 그대로 두면 라인을 읽을 때 답변을 따로 조회한다: dev·staging·prod 는 default_batch_fetch_size(100) 로 라인 100개당 1번,
        // 이 설정이 없는 test 프로필은 라인마다 1번. 주문 + 라인 fetch join 에 같이 넣으면 bag(List) 두 개를 동시에 fetch 할 수 없어 쿼리를 나눈다
        base(
            queryFactory.select(orderLineItem).from(order).join(order.orderLineItems, orderLineItem)
                .leftJoin(orderLineItem.orderOptionAnswers, orderOptionAnswer).fetchJoin(),
            search,
            withStatus = true,
        ).fetch()
        return base(queryFactory.selectFrom(order).distinct().leftJoin(order.orderLineItems, orderLineItem).fetchJoin(), search, withStatus = true)
            .orderBy(order.id.desc())
            .fetch()
    }

    fun counts(search: V2OrderSearch): V2OrderCounts {
        val buckets = listOf(V2OrderStatus.visiblePredicate()) + V2OrderStatus.entries.map { it.predicate() }
        val sums: List<NumberExpression<Long>> = buckets.map { CaseBuilder().`when`(it).then(1L).otherwise(0L).sum() }
        val tuple = base(queryFactory.select(*sums.toTypedArray()).from(order), search, withStatus = false).fetchOne()
        val values = sums.map { tuple?.get(it) ?: 0L }
        return V2OrderCounts(
            all = values[0],
            pendingApprove = values[1 + V2OrderStatus.PENDING_APPROVE.ordinal],
            approved = values[1 + V2OrderStatus.APPROVED.ordinal],
            refused = values[1 + V2OrderStatus.REFUSED.ordinal],
            canceled = values[1 + V2OrderStatus.CANCELED.ordinal],
            failed = values[1 + V2OrderStatus.FAILED.ordinal],
        )
    }

    /** 주문의 공연 id (엔티티를 영속성 컨텍스트에 올리지 않는 스칼라 조회). 없는 주문은 null */
    fun findEventId(orderUuid: String): Long? =
        queryFactory.select(order.eventId).from(order).where(order.uuid.eq(orderUuid)).fetchFirst()

    /** 환불 요청(REFUND_REQUESTED) 상태 주문 수 */
    fun countRefundRequested(eventId: Long): Long =
        queryFactory.select(order.count()).from(order)
            .where(order.eventId.eq(eventId), order.refundStatus.eq(RefundStatus.REFUND_REQUESTED))
            .fetchOne() ?: 0L

    /**
     * 판매금액 합계 = 승인·결제 완료(APPROVED, CONFIRM) 주문의 결제금액 합 (v1 통계와 같은 기준).
     * payment_amount 가 varchar 라 DB SUM 대신 값을 읽어 더한다 (H2 는 문자열 SUM 불가, 공연당 주문 수는 작다)
     */
    fun sumSalesAmount(eventId: Long): Long =
        queryFactory.select(order.totalPaymentInfo.paymentAmount.amount).from(order)
            .where(order.eventId.eq(eventId), order.orderStatus.`in`(OrderStatus.APPROVED, OrderStatus.CONFIRM))
            .fetch()
            .fold(BigDecimal.ZERO) { acc, amount -> acc + (amount ?: BigDecimal.ZERO) }
            .toLong()

    private fun <T> base(query: JPAQuery<T>, search: V2OrderSearch, withStatus: Boolean): JPAQuery<T> {
        val searchFilter = keywordFilter(search)
        // 회원 정보로 검색할 때만 회원 조인 (v1 어드민 목록과 같은 기준)
        val joined = if (searchFilter == null || !searchType(search).needsUserJoin) query else query.join(user).on(user.id.eq(order.userId))
        return joined.where(
            order.eventId.eq(search.eventId),
            if (withStatus) search.status?.predicate() ?: V2OrderStatus.visiblePredicate() else V2OrderStatus.visiblePredicate(),
            searchFilter,
        )
    }

    private fun keywordFilter(search: V2OrderSearch): BooleanExpression? {
        val keyword = search.keyword?.trim()
        if (keyword.isNullOrEmpty()) return null
        return searchType(search).contains(keyword)
    }

    private fun searchType(search: V2OrderSearch): V2OrderSearchType = search.searchType ?: V2OrderSearchType.NAME
}
