package band.gosrock.domain.domains.cart.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
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

    // 연관 관계로 만들면..? 가격정보 도메인 내부로 들일 수 있음
    private Long optionId;

    private Long optionGroupId;
    private String answer;

    @Builder
    public CartOptionAnswer(Long optionId, Long optionGroupId, String answer) {
        this.optionId = optionId;
        this.answer = answer;
        this.optionGroupId = optionGroupId;
    }

    public Money getOptionPrice() {
        // 옵션 엔티티에서 받아오던지...
        // Money getOptionPrice(Option option)
        // option.getPrice();
        // 아니면 복사본을 저장하던지 둘중하나!
        return Money.ZERO;
    }
}
