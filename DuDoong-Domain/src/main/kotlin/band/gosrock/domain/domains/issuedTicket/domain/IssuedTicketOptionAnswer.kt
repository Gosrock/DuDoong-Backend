package band.gosrock.domain.domains.issuedTicket.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.IssuedTicketOptionAnswerVo
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.common.vo.OptionAnswerVo
import band.gosrock.domain.domains.order.domain.OrderOptionAnswer
import band.gosrock.domain.domains.ticket_item.domain.Option
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity(name = "tbl_issued_ticket_option_answer")
class IssuedTicketOptionAnswer(
    var optionId: Long? = null,
    var additionalPrice: Money = Money.ZERO,
    var answer: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issued_ticket_option_answer_id")
    var id: Long? = null
        protected set

    companion object {
        @JvmStatic
        fun from(orderOptionAnswer: OrderOptionAnswer): IssuedTicketOptionAnswer =
            IssuedTicketOptionAnswer(
                optionId = orderOptionAnswer.optionId,
                additionalPrice = orderOptionAnswer.additionalPrice,
                answer = orderOptionAnswer.answer,
            )
    }

    fun toIssuedTicketOptionAnswerVo(): IssuedTicketOptionAnswerVo =
        IssuedTicketOptionAnswerVo.from(this)

    fun getOptionAnswerVo(option: Option): OptionAnswerVo =
        OptionAnswerVo(
            questionDescription = option.getQuestionDescription(),
            optionGroupType = option.getQuestionType(),
            questionName = option.getQuestionName(),
            answer = answer,
            additionalPrice = option.additionalPrice,
        )
}
