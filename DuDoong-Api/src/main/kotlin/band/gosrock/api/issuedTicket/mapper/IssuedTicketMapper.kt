package band.gosrock.api.issuedTicket.mapper

import band.gosrock.api.common.page.PageResponse
import band.gosrock.api.issuedTicket.dto.response.IssuedTicketAdminTableElement
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDetailResponse
import band.gosrock.common.annotation.Mapper
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.repository.condition.FindEventIssuedTicketsCondition
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.User
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@Mapper
class IssuedTicketMapper(
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val userAdaptor: UserAdaptor,
    private val orderAdaptor: OrderAdaptor,
) {

    @Transactional(readOnly = true)
    fun toIssuedTicketAdminTableElementPageResponse(
        page: Pageable,
        condition: FindEventIssuedTicketsCondition,
    ): PageResponse<IssuedTicketAdminTableElement> {
        val issuedTickets = issuedTicketAdaptor.searchIssuedTicket(page, condition)
        val orderUuids = issuedTickets.mapNotNull { it.orderUuid }.distinct()
        val userIds = issuedTickets.mapNotNull { it.getUserId() }.distinct()
        val users = userAdaptor.findUserByIdIn(userIds)
        val orders = orderAdaptor.findByUuidIn(orderUuids)
        val issuedTicketAdminTableElements = issuedTickets.map { issuedTicket ->
            IssuedTicketAdminTableElement.of(
                issuedTicket,
                getUser(users, issuedTicket.getUserId()),
                getOrder(orders, issuedTicket.orderUuid),
            )
        }
        return PageResponse.of(issuedTicketAdminTableElements)
    }

    private fun getOrder(orders: List<Order>, orderUuid: String?): Order {
        return orders.first { it.uuid == orderUuid }
    }

    private fun getUser(users: List<User>, userId: Long?): User {
        return users.first { it.id == userId }
    }

    @Transactional(readOnly = true)
    fun toIssuedTicketDetailResponse(
        currentUserId: Long,
        uuid: String,
    ): RetrieveIssuedTicketDetailResponse {
        val issuedTicket = issuedTicketAdaptor.findForUser(currentUserId, uuid)
        val event = eventAdaptor.findById(issuedTicket.eventId!!)
        return RetrieveIssuedTicketDetailResponse.of(issuedTicket, event)
    }

    @Transactional(readOnly = true)
    fun getIssuedTicket(issuedTicketId: Long): IssuedTicket {
        return issuedTicketAdaptor.queryIssuedTicket(issuedTicketId)
    }
}
