package band.gosrock.domain.domains.order.repository.condition;

import static band.gosrock.domain.domains.order.domain.OrderStatus.APPROVED;
import static band.gosrock.domain.domains.order.domain.OrderStatus.CANCELED;
import static band.gosrock.domain.domains.order.domain.OrderStatus.CONFIRM;
import static band.gosrock.domain.domains.order.domain.OrderStatus.PENDING_APPROVE;
import static band.gosrock.domain.domains.order.domain.OrderStatus.REFUND;
import static band.gosrock.domain.domains.order.domain.QOrder.order;

import com.querydsl.core.types.dsl.BooleanExpression;

/** 어드민 테이블의 주문 상태 검색 조건 지정을 위함. */
public enum AdminTableOrderFilterType {
    APPROVE_WAITING(order.orderStatus.eq(PENDING_APPROVE)),
    CONFIRMED(order.orderStatus.in(CONFIRM, APPROVED, CANCELED, REFUND));

    private final BooleanExpression expression;

    AdminTableOrderFilterType(BooleanExpression expression) {
        this.expression = expression;
    }

    public BooleanExpression getFilter() {
        return expression;
    }
}
