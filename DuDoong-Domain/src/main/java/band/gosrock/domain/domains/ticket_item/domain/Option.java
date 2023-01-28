package band.gosrock.domain.domains.ticket_item.domain;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.ticket_item.exception.NotCorrectOptionAnswerException;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_option")
public class Option {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Long id;

    private String answer;

    private Money additionalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", updatable = false)
    private OptionGroup optionGroup;

    @Builder
    public Option(String answer, Money additionalPrice, OptionGroup optionGroup) {
        this.answer = answer;
        this.additionalPrice = additionalPrice;
        this.optionGroup = optionGroup;
    }

    public static Option create(String answer, Money additionalPrice, OptionGroup optionGroup) {
        return Option.builder()
                .answer(answer)
                .additionalPrice(additionalPrice)
                .optionGroup(optionGroup)
                .build();
    }

    public void setOptionGroup(OptionGroup optionGroup) {
        this.optionGroup = optionGroup;
    }

    public Long getOptionGroupId() {
        return this.optionGroup.getId();
    }

    public String getQuestionDescription() {
        return this.optionGroup.getDescription();
    }

    public String getQuestionName() {
        return this.optionGroup.getName();
    }

    public OptionGroupType getQuestionType() {
        return this.optionGroup.getType();
    }

    public void validCorrectAnswer(String answer) {
        OptionGroupType type = optionGroup.getType();
        if (type == OptionGroupType.TRUE_FALSE) {
            if (!isAnswerTrueFalse(answer)) {
                throw NotCorrectOptionAnswerException.EXCEPTION;
            }
        }
    }

    private Boolean isAnswerTrueFalse(String answer) {
        return answer.equals("예") || answer.equals("아니오");
    }
}
