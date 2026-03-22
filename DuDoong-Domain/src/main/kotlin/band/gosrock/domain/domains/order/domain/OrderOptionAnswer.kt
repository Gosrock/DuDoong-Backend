package band.gosrock.domain.domains.order.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.common.vo.OptionAnswerVo
import band.gosrock.domain.domains.cart.domain.CartOptionAnswer
import band.gosrock.domain.domains.ticket_item.domain.Option
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

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
    }

    fun getOptionAnswerVo(option: Option): OptionAnswerVo =
        OptionAnswerVo(
            questionDescription = option.getQuestionDescription(),
            answer = answer,
            optionGroupType = option.getQuestionType(),
            questionName = option.getQuestionName(),
            additionalPrice = additionalPrice,
        )
}
