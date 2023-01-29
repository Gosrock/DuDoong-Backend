package band.gosrock.domain.domains.cart.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.OptionAnswerVo;
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
@Entity(name = "tbl_cart_option_answer")
public class CartOptionAnswer extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_option_answer_id")
    private Long id;

    @Column(nullable = false)
    private Long optionId;

    @Column(nullable = false)
    private Money additionalPrice = Money.ZERO;

    private String answer;
    /** ---------------------------- 생성 관련 메서드 ---------------------------------- */
    @Builder
    public CartOptionAnswer(Option option, String answer) {
        this.optionId = option.getId();
        this.additionalPrice = option.getAdditionalPrice();
        this.answer = answer;
    }

    /** ---------------------------- 커맨드 메서드 ---------------------------------- */

    /** ---------------------------- 검증 메서드 ---------------------------------- */

    /** ---------------------------- 조회용 메서드 ---------------------------------- */
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
