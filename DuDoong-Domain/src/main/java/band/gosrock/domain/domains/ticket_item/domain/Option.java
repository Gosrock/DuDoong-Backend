package band.gosrock.domain.domains.ticket_item.domain;


import band.gosrock.domain.common.vo.Money;
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
    public Option(String answer, Money additionalPrice) {
        this.answer = answer;
        this.additionalPrice = additionalPrice;
    }

    public void setOptionGroup(OptionGroup optionGroup) {
        this.optionGroup = optionGroup;
    }

    public Long getOptionGroupId(){
        return this.optionGroup.getId();
    }
}
