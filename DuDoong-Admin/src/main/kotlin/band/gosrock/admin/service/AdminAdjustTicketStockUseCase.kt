package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminTicketItemResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor

@UseCase
class AdminAdjustTicketStockUseCase(
    private val ticketItemAdaptor: TicketItemAdaptor,
) {
    @RedissonLock(LockName = "티켓관리", identifier = "ticketItemId")
    fun execute(ticketItemId: Long, delta: Long): AdminTicketItemResponse {
        val ticketItem = ticketItemAdaptor.queryTicketItem(ticketItemId)
        ticketItem.adminAdjustStock(delta)
        ticketItemAdaptor.save(ticketItem)
        return AdminTicketItemResponse.from(ticketItem)
    }
}
