package band.gosrock.domain.domains.order.repository.condition

import band.gosrock.domain.domains.order.domain.OrderStatus.APPROVED
import band.gosrock.domain.domains.order.domain.OrderStatus.CANCELED
import band.gosrock.domain.domains.order.domain.OrderStatus.CONFIRM
import band.gosrock.domain.domains.order.domain.OrderStatus.PENDING_APPROVE
import band.gosrock.domain.domains.order.domain.OrderStatus.REFUND
import band.gosrock.domain.domains.order.domain.QOrder.order
import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.domain.QUser.user
import com.querydsl.core.types.dsl.BooleanExpression

/** 어드민 테이블의 주문 상태 검색 조건 지정을 위함. */
enum class AdminTableOrderFilterType(
    private val expression: BooleanExpression,
    private val showDeleteUserExpression: BooleanExpression?
) {
    APPROVE_WAITING(
        order.orderStatus.eq(PENDING_APPROVE),
        user.accountState.ne(AccountState.DELETED)
    ),
    // 완료된 주문을 가져오는건 지워진 유저도 불러와야한다.
    CONFIRMED(
        order.orderStatus.`in`(CONFIRM, APPROVED, CANCELED, REFUND),
        null
    );

    fun getFilter(): BooleanExpression = expression

    fun showDeleteUserExpression(): BooleanExpression? = showDeleteUserExpression
}
