package band.gosrock.domain.domains.order.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.domains.cart.domain.CartOptionAnswer;
import band.gosrock.domain.domains.ticket_item.domain.Option;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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

    // 연관 관계로 만들면..? 가격정보 도메인 내부로 들일 수 있음
    @JoinColumn(name = "option_id", updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Option option;

    private String answer;

    @Builder
    public OrderOptionAnswer(Option option, String answer) {
        this.option = option;
        this.answer = answer;
    }

    public static OrderOptionAnswer from(CartOptionAnswer cartOptionAnswer) {
        return OrderOptionAnswer.builder()
                .answer(cartOptionAnswer.getAnswer())
                .option(cartOptionAnswer.getOption())
                .build();
    }

    protected Money getOptionPrice() {
        return option.getAdditionalPrice();
    }

    protected String getQuestionDescription() {
        return option.getOptionGroup().getDescription();
    }

    protected String getQuestionName() {
        return option.getOptionGroup().getName();
    }

    protected OptionGroupType getQuestionType() {
        return option.getOptionGroup().getType();
    }

    public OptionAnswerVo getOptionAnswerVo() {
        return OptionAnswerVo.builder()
                .questionDescription(getQuestionDescription())
                .optionGroupType(getQuestionType())
                .questionName(getQuestionName())
                .additionalPrice(getOptionPrice().toString())
                .build();
    }
}
