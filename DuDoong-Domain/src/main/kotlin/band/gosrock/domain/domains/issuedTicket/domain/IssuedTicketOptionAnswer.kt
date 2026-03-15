package band.gosrock.domain.domains.issuedTicket.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.IssuedTicketOptionAnswerVo
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.common.vo.OptionAnswerVo
import band.gosrock.domain.domains.order.domain.OrderOptionAnswer
import band.gosrock.domain.domains.ticket_item.domain.Option
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity(name = "tbl_issued_ticket_option_answer")
class IssuedTicketOptionAnswer() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issued_ticket_option_answer_id")
    var id: Long? = null
        protected set

    var optionId: Long? = null
        protected set

    var additionalPrice: Money = Money.ZERO
        protected set

    var answer: String? = null
        protected set

    constructor(optionId: Long?, additionalPrice: Money, answer: String?) : this() {
        this.optionId = optionId
        this.additionalPrice = additionalPrice
        this.answer = answer
    }

    companion object {
        @JvmStatic
        fun from(orderOptionAnswer: OrderOptionAnswer): IssuedTicketOptionAnswer =
            IssuedTicketOptionAnswer(
                optionId = orderOptionAnswer.optionId,
                additionalPrice = orderOptionAnswer.additionalPrice,
                answer = orderOptionAnswer.answer,
            )

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var optionId: Long? = null
        private var additionalPrice: Money = Money.ZERO
        private var answer: String? = null

        fun optionId(optionId: Long?) = apply { this.optionId = optionId }
        fun additionalPrice(additionalPrice: Money) = apply { this.additionalPrice = additionalPrice }
        fun answer(answer: String?) = apply { this.answer = answer }

        fun build(): IssuedTicketOptionAnswer = IssuedTicketOptionAnswer(optionId, additionalPrice, answer)
    }

    fun toIssuedTicketOptionAnswerVo(): IssuedTicketOptionAnswerVo =
        IssuedTicketOptionAnswerVo.from(this)

    fun getOptionAnswerVo(option: Option): OptionAnswerVo =
        OptionAnswerVo.builder()
            .questionDescription(option.getQuestionDescription())
            .optionGroupType(option.getQuestionType())
            .questionName(option.getQuestionName())
            .answer(answer)
            .additionalPrice(option.additionalPrice)
            .build()
}
