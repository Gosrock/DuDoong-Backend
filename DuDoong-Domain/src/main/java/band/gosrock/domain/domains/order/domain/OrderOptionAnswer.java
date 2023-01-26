package band.gosrock.domain.domains.order.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.domains.cart.domain.CartOptionAnswer;
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
@Entity(name = "tbl_order_option_answer")
public class OrderOptionAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_option_answer_id")
    private Long id;

    private Long optionId;

    private Money additionalPrice = Money.ZERO;

    private String answer;
    /** ---------------------------- 생성 관련 메서드 ---------------------------------- */
    @Builder
    public OrderOptionAnswer(Option option, String answer) {
        this.optionId = option.getId();
        this.additionalPrice = option.getAdditionalPrice();
        this.answer = answer;
    }

    public static OrderOptionAnswer from(CartOptionAnswer cartOptionAnswer) {
        return OrderOptionAnswer.builder()
                .answer(cartOptionAnswer.getAnswer())
                .option(cartOptionAnswer.getOption())
                .build();
    }

    /** ---------------------------- 커맨드 메서드 ---------------------------------- */

    /** ---------------------------- 검증 메서드 ---------------------------------- */

    /** ---------------------------- 조회용 메서드 ---------------------------------- */
    //    protected String getQuestionDescription() {
    //        return option.getQuestionDescription();
    //    }
    //
    //    protected String getQuestionName() {
    //        return option.getQuestionName();
    //    }
    //
    //    protected OptionGroupType getQuestionType() {
    //        return option.getQuestionType();
    //    }

    public OptionAnswerVo getOptionAnswerVo(Option option) {
        return OptionAnswerVo.builder()
                .questionDescription(option.getQuestionDescription())
                .answer(answer)
                .optionGroupType(option.getQuestionType())
                .questionName(option.getQuestionName())
                .additionalPrice(additionalPrice)
                .build();
    }
}
