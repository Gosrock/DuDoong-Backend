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

    @Builder
    public Option(String answer, Money additionalPrice) {
        this.answer = answer;
        this.additionalPrice = additionalPrice;
    }
}
