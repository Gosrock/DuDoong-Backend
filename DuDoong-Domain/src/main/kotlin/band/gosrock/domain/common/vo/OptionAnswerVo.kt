package band.gosrock.domain.common.vo

import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType

data class OptionAnswerVo(
    val optionGroupType: OptionGroupType?,
    val questionName: String?,
    val questionDescription: String?,
    val answer: String?,
    val additionalPrice: Money?,
)
