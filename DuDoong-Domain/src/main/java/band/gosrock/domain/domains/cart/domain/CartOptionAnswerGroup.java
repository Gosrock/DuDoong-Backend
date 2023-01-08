package band.gosrock.domain.domains.cart.domain;


import band.gosrock.domain.common.vo.Money;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_cart_option_group")
public class CartOptionAnswerGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_option_group_id")
    private Long id;

    private Long optionGroupId;

    // 객관식 다지선다 가능할수도 있으니? 리스트형
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_option_group_id")
    private List<CartOptionAnswer> cartOptionAnswers = new ArrayList<>();

    public Money getOptionAnswersPrice() {
        return cartOptionAnswers.stream()
                .map(CartOptionAnswer::getOptionPrice)
                .reduce(Money.ZERO, Money::plus);
    }
}
