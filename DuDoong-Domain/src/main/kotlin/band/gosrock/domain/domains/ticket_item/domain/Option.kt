package band.gosrock.domain.domains.ticket_item.domain

import band.gosrock.common.consts.DuDoongStatic.KR_NO
import band.gosrock.common.consts.DuDoongStatic.KR_YES
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.exception.NotCorrectOptionAnswerException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity(name = "tbl_option")
class Option(
    var answer: String? = null,
    var additionalPrice: Money? = null,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", updatable = false)
    var optionGroup: OptionGroup? = null
        protected set

    constructor(answer: String?, additionalPrice: Money?, optionGroup: OptionGroup?) : this(answer = answer, additionalPrice = additionalPrice) {
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
    }
}
