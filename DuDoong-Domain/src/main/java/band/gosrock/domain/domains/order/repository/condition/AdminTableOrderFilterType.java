package band.gosrock.domain.domains.order.repository.condition;

import static band.gosrock.domain.domains.order.domain.OrderStatus.APPROVED;
import static band.gosrock.domain.domains.order.domain.OrderStatus.CANCELED;
import static band.gosrock.domain.domains.order.domain.OrderStatus.CONFIRM;
import static band.gosrock.domain.domains.order.domain.OrderStatus.PENDING_APPROVE;
import static band.gosrock.domain.domains.order.domain.OrderStatus.REFUND;
import static band.gosrock.domain.domains.order.domain.QOrder.order;
import static band.gosrock.domain.domains.user.domain.QUser.user;

import band.gosrock.domain.domains.user.domain.AccountState;
import com.querydsl.core.types.dsl.BooleanExpression;

/** 어드민 테이블의 주문 상태 검색 조건 지정을 위함. */
public enum AdminTableOrderFilterType {
    APPROVE_WAITING(
            order.orderStatus.eq(PENDING_APPROVE), user.accountState.ne(AccountState.DELETED)),
    // 완료된 주문을 가져오는건 지워진 유저도 불러와야한다.
    CONFIRMED(order.orderStatus.in(CONFIRM, APPROVED, CANCELED, REFUND), null);

    private final BooleanExpression expression;
    private final BooleanExpression showDeleteUserExpression;

    AdminTableOrderFilterType(
            BooleanExpression expression, BooleanExpression showDeleteUserExpression) {
        this.expression = expression;
        this.showDeleteUserExpression = showDeleteUserExpression;
    }

    public BooleanExpression getFilter() {
        return expression;
    }

    public BooleanExpression showDeleteUserExpression() {
        return showDeleteUserExpression;
    }
}
