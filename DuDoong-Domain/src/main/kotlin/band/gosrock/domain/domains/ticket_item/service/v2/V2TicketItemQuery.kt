package band.gosrock.domain.domains.ticket_item.service.v2

import band.gosrock.domain.domains.ticket_item.domain.QItemOptionGroup.itemOptionGroup
import band.gosrock.domain.domains.ticket_item.domain.QOptionGroup.optionGroup
import band.gosrock.domain.domains.ticket_item.domain.QTicketItem.ticketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketItemStatus
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component

/** v2 티켓 조회 쿼리. v2 에서만 쓴다 */
@Component
class V2TicketItemQuery(private val queryFactory: JPAQueryFactory) {

    /**
     * 공연의 유효 티켓 + 붙은 옵션 그룹을 한 번에 (P-5, #716 L-3).
     * `TicketItem.itemOptionGroups` 는 EAGER 라 일반 조회는 티켓마다 옵션 연결을 따로 읽는다(N+1) — fetch join 으로 한 쿼리.
     * 옵션 그룹의 선택지(options, LAZY)는 default_batch_fetch_size 로 일괄 로딩. id 순
     */
    fun findValidWithOptionGroupsByEventId(eventId: Long): List<TicketItem> =
        queryFactory.selectFrom(ticketItem).distinct()
            .leftJoin(ticketItem.itemOptionGroups, itemOptionGroup).fetchJoin()
            .leftJoin(itemOptionGroup.optionGroup, optionGroup).fetchJoin()
            .where(ticketItem.eventId.eq(eventId), ticketItem.ticketItemStatus.eq(TicketItemStatus.VALID))
            .orderBy(ticketItem.id.asc())
            .fetch()
}
