package band.gosrock.domain.domains.ticket_item.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.ticket_item.adaptor.OptionGroupAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.repository.TicketItemRepository
import org.springframework.transaction.annotation.Transactional

@DomainService
@Transactional(readOnly = true)
class ItemOptionGroupService(
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val optionGroupAdaptor: OptionGroupAdaptor,
    private val ticketItemRepository: TicketItemRepository
) {

    @RedissonLock(LockName = "티켓관리", identifier = "ticketItemId")
    fun addItemOptionGroup(ticketItemId: Long, optionGroupId: Long, eventId: Long): TicketItem {
        val ticketItem = ticketItemAdaptor.queryTicketItem(ticketItemId)
        val optionGroup = optionGroupAdaptor.queryOptionGroup(optionGroupId)

        // 해당 eventId에 속해 있는 티켓 아이템, 옵션그룹이 맞는지 확인
        ticketItem.validateEventId(eventId)
        optionGroup.validateEventId(eventId)

        ticketItem.addItemOptionGroup(optionGroup)
        return ticketItemRepository.save(ticketItem)
    }

    @RedissonLock(LockName = "티켓관리", identifier = "ticketItemId")
    fun removeItemOptionGroup(ticketItemId: Long, optionGroupId: Long, eventId: Long): TicketItem {
        val ticketItem = ticketItemAdaptor.queryTicketItem(ticketItemId)
        val optionGroup = optionGroupAdaptor.queryOptionGroup(optionGroupId)

        // 해당 eventId에 속해 있는 티켓 아이템, 옵션그룹이 맞는지 확인
        ticketItem.validateEventId(eventId)
        optionGroup.validateEventId(eventId)

        ticketItem.removeItemOptionGroup(optionGroup)
        return ticketItemRepository.save(ticketItem)
    }
}
