package band.gosrock.domain.domains.ticket_item.domain

import band.gosrock.common.consts.DuDoongStatic.KR_NO
import band.gosrock.common.consts.DuDoongStatic.KR_YES
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.exception.NotCorrectOptionAnswerException
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity(name = "tbl_option")
class Option() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    var id: Long? = null
        protected set

    var answer: String? = null
        protected set

    var additionalPrice: Money? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", updatable = false)
    var optionGroup: OptionGroup? = null
        protected set

    constructor(answer: String?, additionalPrice: Money?, optionGroup: OptionGroup?) : this() {
        this.answer = answer
        this.additionalPrice = additionalPrice
        this.optionGroup = optionGroup
    }

    fun updateOptionGroup(optionGroup: OptionGroup?) {
        this.optionGroup = optionGroup
    }

    fun getOptionGroupId(): Long? = this.optionGroup?.id

    fun getQuestionDescription(): String? = this.optionGroup?.description

    fun getQuestionName(): String? = this.optionGroup?.name

    fun getQuestionType(): OptionGroupType? = this.optionGroup?.type

    fun validCorrectAnswer(answer: String) {
        val type = optionGroup?.type
        if (type == OptionGroupType.TRUE_FALSE) {
            if (!isAnswerTrueFalse(answer)) {
                throw NotCorrectOptionAnswerException.EXCEPTION
            }
        }
    }

    private fun isAnswerTrueFalse(answer: String): Boolean =
        answer == KR_YES || answer == KR_NO

    companion object {
        @JvmStatic
        fun create(answer: String?, additionalPrice: Money?, optionGroup: OptionGroup?): Option =
            Option(answer, additionalPrice, optionGroup)

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var answer: String? = null
        private var additionalPrice: Money? = null
        private var optionGroup: OptionGroup? = null

        fun answer(answer: String?) = apply { this.answer = answer }
        fun additionalPrice(price: Money?) = apply { this.additionalPrice = price }
        fun optionGroup(optionGroup: OptionGroup?) = apply { this.optionGroup = optionGroup }
        fun build() = Option(answer, additionalPrice, optionGroup)
    }
}
