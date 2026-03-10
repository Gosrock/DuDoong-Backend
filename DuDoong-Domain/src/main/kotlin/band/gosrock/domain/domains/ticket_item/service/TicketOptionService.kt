package band.gosrock.domain.domains.ticket_item.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.domains.ticket_item.adaptor.OptionGroupAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup
import org.springframework.transaction.annotation.Transactional

@DomainService
@Transactional(readOnly = true)
class TicketOptionService(
    private val optionGroupAdaptor: OptionGroupAdaptor,
    private val ticketItemAdaptor: TicketItemAdaptor
) {

    @Transactional
    fun createTicketOption(optionGroup: OptionGroup): OptionGroup {
        return optionGroupAdaptor.save(optionGroup)
    }

    @Transactional
    fun softDeleteOptionGroup(eventId: Long, optionGroupId: Long) {
        val optionGroup = optionGroupAdaptor.queryOptionGroup(optionGroupId)
        // 해당 eventId에 속해 있는 옵션그룹이 맞는지 확인
        optionGroup.validateEventId(eventId)

        val ticketItems = ticketItemAdaptor.findAllByEventId(eventId)
        optionGroup.softDeleteOptionGroup(ticketItems)
        optionGroupAdaptor.save(optionGroup)
    }
}
