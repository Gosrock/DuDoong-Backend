package band.gosrock.domain.domains.ticket_item.domain;


import band.gosrock.domain.common.vo.Money;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_option_answer")
public class OptionAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_answer_id")
    private Long id;

    private Long optionId;

    private String answer;

    private Money additionalPrice;

    @Builder
    public OptionAnswer(Long optionId, String answer, Money additionalPrice) {
        this.optionId = optionId;
        this.answer = answer;
        this.additionalPrice = additionalPrice;
    }
}
