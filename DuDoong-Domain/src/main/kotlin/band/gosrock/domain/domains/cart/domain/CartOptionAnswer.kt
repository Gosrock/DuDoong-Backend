package band.gosrock.domain.domains.cart.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.common.vo.OptionAnswerVo
import band.gosrock.domain.domains.ticket_item.domain.Option
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity(name = "tbl_cart_option_answer")
class CartOptionAnswer() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_option_answer_id")
    var id: Long? = null
        protected set

    @Column(nullable = false)
    var optionId: Long? = null
        protected set

    @Column(nullable = false)
    var additionalPrice: Money = Money.ZERO
        protected set

    var answer: String? = null
        protected set

    companion object {
        @JvmStatic
        fun of(option: Option, answer: String): CartOptionAnswer = CartOptionAnswer().apply {
            this.optionId = option.id
            this.additionalPrice = option.additionalPrice ?: Money.ZERO
            this.answer = answer
        }

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var option: Option? = null
        private var answer: String? = null
        fun option(option: Option) = apply { this.option = option }
        fun answer(answer: String?) = apply { this.answer = answer }
        fun build(): CartOptionAnswer = of(option!!, answer ?: "")
    }

    fun getOptionAnswerVo(option: Option): OptionAnswerVo =
        OptionAnswerVo.builder()
            .questionDescription(option.getQuestionDescription())
            .optionGroupType(option.getQuestionType())
            .questionName(option.getQuestionName())
            .answer(answer)
            .additionalPrice(option.additionalPrice)
            .build()
}
