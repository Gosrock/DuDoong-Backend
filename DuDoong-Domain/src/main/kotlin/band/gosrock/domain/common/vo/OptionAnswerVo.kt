package band.gosrock.domain.common.vo

import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType

data class OptionAnswerVo(
    val optionGroupType: OptionGroupType?,
    val questionName: String?,
    val questionDescription: String?,
    val answer: String?,
    val additionalPrice: Money?,
) {
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var optionGroupType: OptionGroupType? = null
        private var questionName: String? = null
        private var questionDescription: String? = null
        private var answer: String? = null
        private var additionalPrice: Money? = null

        fun optionGroupType(v: OptionGroupType?) = apply { optionGroupType = v }
        fun questionName(v: String?) = apply { questionName = v }
        fun questionDescription(v: String?) = apply { questionDescription = v }
        fun answer(v: String?) = apply { answer = v }
        fun additionalPrice(v: Money?) = apply { additionalPrice = v }
        fun build() = OptionAnswerVo(optionGroupType, questionName, questionDescription, answer, additionalPrice)
    }
}
