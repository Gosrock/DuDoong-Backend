package band.gosrock.domain.domains.issuedTicket.repository;


import band.gosrock.domain.domains.event.domain.QEvent;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.QIssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.QIssuedTicketOptionAnswer;
import band.gosrock.domain.domains.issuedTicket.dto.condtion.IssuedTicketCondition;
import band.gosrock.domain.domains.ticket_item.domain.QTicketItem;
import band.gosrock.domain.domains.user.domain.QUser;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

public class IssuedTicketCustomRepositoryImpl implements IssuedTicketCustomRepository {

    private final JPAQueryFactory queryFactory;

    public IssuedTicketCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.queryFactory = jpaQueryFactory;
    }

    QIssuedTicket qIssuedTicket = QIssuedTicket.issuedTicket;
    QEvent qEvent = QEvent.event;
    QUser qUser = QUser.user;
    QTicketItem qTicketItem = QTicketItem.ticketItem;
    QIssuedTicketOptionAnswer qIssuedTicketOptionAnswer =
            QIssuedTicketOptionAnswer.issuedTicketOptionAnswer;

    @Override
    public Page<IssuedTicket> searchToPage(IssuedTicketCondition condition, Pageable pageable) {
        List<IssuedTicket> issuedTickets =
                queryFactory
                        .selectFrom(qIssuedTicket)
                        .leftJoin(qIssuedTicket.event, qEvent)
                        .fetchJoin()
                        .leftJoin(qIssuedTicket.user, qUser)
                        .fetchJoin()
                        .leftJoin(qIssuedTicket.ticketItem, qTicketItem)
                        .fetchJoin()
                        .leftJoin(
                                qIssuedTicket.issuedTicketOptionAnswers, qIssuedTicketOptionAnswer)
                        .fetchJoin()
                        .where(
                                eventIdEq(condition.getEventId()),
                                userNameContains(condition.getUserName()),
                                phoneNumberContains(condition.getPhoneNumber()))
                        .offset(pageable.getOffset())
                        //                        .limit(pageable.getPageSize())
                        .fetch();

        JPAQuery<Long> countQuery =
                queryFactory
                        .select(qIssuedTicket.count())
                        .from(qIssuedTicket)
                        .where(
                                eventIdEq(condition.getEventId()),
                                userNameContains(condition.getUserName()),
                                phoneNumberContains(condition.getPhoneNumber()));

        return PageableExecutionUtils.getPage(issuedTickets, pageable, countQuery::fetchOne);
    }

    @Override
    public Optional<IssuedTicket> find(Long issuedTicketId) {
        IssuedTicket issuedTicket =
                queryFactory
                        .selectFrom(qIssuedTicket)
                        .leftJoin(qIssuedTicket.event, qEvent)
                        .fetchJoin()
                        .leftJoin(qIssuedTicket.user, qUser)
                        .fetchJoin()
                        .leftJoin(qIssuedTicket.ticketItem, qTicketItem)
                        .fetchJoin()
                        .leftJoin(
                                qIssuedTicket.issuedTicketOptionAnswers, qIssuedTicketOptionAnswer)
                        .fetchJoin()
                        .where(qIssuedTicket.id.eq(issuedTicketId))
                        .fetchOne();
        return Optional.ofNullable(issuedTicket);
    }

    private BooleanExpression eventIdEq(Long eventId) {
        return eventId == null ? null : qIssuedTicket.event.id.eq(eventId);
    }

    private BooleanExpression userNameContains(String userName) {
        return userName == null ? null : qIssuedTicket.user.profile.name.contains(userName);
    }

    private BooleanExpression phoneNumberContains(String phoneNumber) {
        return phoneNumber == null
                ? null
                : qIssuedTicket.user.profile.phoneNumber.contains(phoneNumber);
    }
}
