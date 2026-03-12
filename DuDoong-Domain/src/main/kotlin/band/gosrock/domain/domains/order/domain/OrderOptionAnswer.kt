package band.gosrock.domain.domains.order.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.common.vo.OptionAnswerVo
import band.gosrock.domain.domains.cart.domain.CartOptionAnswer
import band.gosrock.domain.domains.ticket_item.domain.Option
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity(name = "tbl_order_option_answer")
class OrderOptionAnswer() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_option_answer_id")
    var id: Long? = null
        protected set

    var optionId: Long? = null
        protected set

    var additionalPrice: Money = Money.ZERO
        protected set

    var answer: String? = null
        protected set

    companion object {
        @JvmStatic
        fun from(cartOptionAnswer: CartOptionAnswer): OrderOptionAnswer = OrderOptionAnswer().apply {
            answer = cartOptionAnswer.answer
            optionId = cartOptionAnswer.optionId
            additionalPrice = cartOptionAnswer.additionalPrice
        }

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var optionId: Long? = null
        private var additionalPrice: band.gosrock.domain.common.vo.Money = band.gosrock.domain.common.vo.Money.ZERO
        private var answer: String? = null
        fun optionId(optionId: Long) = apply { this.optionId = optionId }
        fun additionalPrice(additionalPrice: band.gosrock.domain.common.vo.Money) = apply { this.additionalPrice = additionalPrice }
        fun answer(answer: String?) = apply { this.answer = answer }
        fun build(): OrderOptionAnswer = OrderOptionAnswer().apply {
            this.optionId = this@Builder.optionId
            this.additionalPrice = this@Builder.additionalPrice
            this.answer = this@Builder.answer
        }
    }

    fun getOptionAnswerVo(option: Option): OptionAnswerVo =
        OptionAnswerVo.builder()
            .questionDescription(option.getQuestionDescription())
            .answer(answer)
            .optionGroupType(option.getQuestionType())
            .questionName(option.getQuestionName())
            .additionalPrice(additionalPrice)
            .build()
}
