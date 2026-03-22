package band.gosrock.admin.service

import band.gosrock.admin.model.dto.request.AdminUpdateTicketItemRequest
import band.gosrock.admin.model.dto.response.AdminTicketItemResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminUpdateTicketItemUseCase(
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val adminAuthValidator: AdminAuthValidator,
) {

    @Transactional
    fun execute(userId: Long, eventId: Long, ticketItemId: Long, request: AdminUpdateTicketItemRequest): AdminTicketItemResponse {
        adminAuthValidator.validateAdminOrAbove(userId)
        val ticketItem = ticketItemAdaptor.queryTicketItem(ticketItemId)
        ticketItem.validateEventId(eventId)

        val money = request.price?.let { Money(it) }

        // 어드민은 이벤트 상태 체크 없이 수정 가능
        ticketItem.adminUpdate(
            name = request.name,
            description = request.description,
            price = money,
            quantity = request.quantity,
            purchaseLimit = request.purchaseLimit,
        )
        ticketItemAdaptor.save(ticketItem)

        return AdminTicketItemResponse.from(ticketItem)
    }
}
