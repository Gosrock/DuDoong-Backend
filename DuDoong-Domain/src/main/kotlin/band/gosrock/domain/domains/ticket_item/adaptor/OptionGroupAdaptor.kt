package band.gosrock.domain.domains.ticket_item.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupStatus
import band.gosrock.domain.domains.ticket_item.exception.OptionGroupNotFoundException
import band.gosrock.domain.domains.ticket_item.repository.OptionGroupRepository

@Adaptor
class OptionGroupAdaptor(
    private val optionGroupRepository: OptionGroupRepository
) {

    fun queryOptionGroup(optionGroupId: Long): OptionGroup {
        return optionGroupRepository
            .findByIdAndOptionGroupStatus(optionGroupId, OptionGroupStatus.VALID)
            .orElseThrow { OptionGroupNotFoundException.EXCEPTION }
    }

    fun findAllByEventId(eventId: Long): List<OptionGroup> {
        return optionGroupRepository.findAllByEventIdAndOptionGroupStatus(
            eventId, OptionGroupStatus.VALID
        )
    }

    fun save(optionGroup: OptionGroup): OptionGroup {
        return optionGroupRepository.save(optionGroup)
    }
}
