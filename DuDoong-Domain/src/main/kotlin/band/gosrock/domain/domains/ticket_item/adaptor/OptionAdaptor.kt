package band.gosrock.domain.domains.ticket_item.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.ticket_item.domain.Option
import band.gosrock.domain.domains.ticket_item.exception.OptionNotFoundException
import band.gosrock.domain.domains.ticket_item.repository.OptionRepository

@Adaptor
class OptionAdaptor(
    private val optionRepository: OptionRepository
) {

    fun queryOption(optionId: Long): Option {
        return optionRepository
            .findById(optionId)
            .orElseThrow { OptionNotFoundException.EXCEPTION }
    }

    fun findAllByIds(ids: List<Long>): List<Option> {
        return optionRepository.findAllById(ids)
    }

    /** 옵션 그룹을 함께 읽는다 (fetch join) */
    fun findAllWithGroupByIds(ids: List<Long>): List<Option> = optionRepository.findAllWithGroupByIdIn(ids)
}
