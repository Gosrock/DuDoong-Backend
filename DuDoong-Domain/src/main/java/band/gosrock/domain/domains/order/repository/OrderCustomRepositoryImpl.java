package band.gosrock.domain.domains.order.repository;

import static band.gosrock.domain.domains.event.domain.QEvent.event;
import static band.gosrock.domain.domains.issuedTicket.domain.QIssuedTicket.issuedTicket;
import static band.gosrock.domain.domains.issuedTicket.domain.QIssuedTicketOptionAnswer.issuedTicketOptionAnswer;
import static band.gosrock.domain.domains.order.domain.QOrder.order;
import static band.gosrock.domain.domains.ticket_item.domain.QTicketItem.ticketItem;
import static band.gosrock.domain.domains.user.domain.QUser.user;

import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus;
import band.gosrock.domain.domains.issuedTicket.dto.condition.IssuedTicketCondition;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketCustomRepository;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.QOrder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

@RequiredArgsConstructor
public class OrderCustomRepositoryImpl implements OrderCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Order> getOrdersWithPage(Long userId , Pageable pageable) {
        List<Order> issuedTickets =
                queryFactory
                        .selectFrom(order)
                        .where(
                                userIdEq(userId)
                            )
                        .orderBy(order.id.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();

        JPAQuery<Long> countQuery =
                queryFactory
                        .select(order.count())
                        .from(order)
                        .where(
                            userIdEq(userId));

        return PageableExecutionUtils.getPage(issuedTickets, pageable, countQuery::fetchOne);
    }


    private BooleanExpression userIdEq(Long userId) {
        return userId == null ? null : order.userId.eq(userId);
    }
}
