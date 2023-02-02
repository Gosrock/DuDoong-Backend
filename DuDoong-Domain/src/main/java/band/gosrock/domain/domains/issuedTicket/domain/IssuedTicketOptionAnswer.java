package band.gosrock.domain.domains.issuedTicket.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.IssuedTicketOptionAnswerVo;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.domains.order.domain.OrderOptionAnswer;
import band.gosrock.domain.domains.ticket_item.domain.Option;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_issued_ticket_option_answer")
public class IssuedTicketOptionAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issued_ticket_option_answer_id")
    private Long id;

    private Long optionId;

    private Money additionalPrice = Money.ZERO;

    /*
    답변
     */
    private String answer;

    @Builder
    public IssuedTicketOptionAnswer(Long optionId, Money additionalPrice, String answer) {
        this.optionId = optionId;
        this.additionalPrice = additionalPrice;
        this.answer = answer;
    }

    public static IssuedTicketOptionAnswer from(OrderOptionAnswer orderOptionAnswer) {
        return IssuedTicketOptionAnswer.builder()
                .optionId(orderOptionAnswer.getOptionId())
                .additionalPrice(orderOptionAnswer.getAdditionalPrice())
                .answer(orderOptionAnswer.getAnswer())
                .build();
    }

    public IssuedTicketOptionAnswerVo toIssuedTicketOptionAnswerVo() {
        return IssuedTicketOptionAnswerVo.from(this);
    }

    /*
    발급 티켓의 옵션 조회용 메서드
     */
    public OptionAnswerVo getOptionAnswerVo(Option option) {
        return OptionAnswerVo.builder()
                .questionDescription(option.getQuestionDescription())
                .optionGroupType(option.getQuestionType())
                .questionName(option.getQuestionName())
                .answer(answer)
                .additionalPrice(option.getAdditionalPrice())
                .build();
    }
}
