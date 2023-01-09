package band.gosrock.domain.domains.cart.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_cart")
public class Cart extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long id;

    private Long userId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private List<CartLineItem> cartLineItems = new ArrayList<>();

    @Builder
    public Cart(Long userId, List<CartLineItem> cartLineItems) {
        this.userId = userId;
        this.cartLineItems.addAll(cartLineItems);
    }

    public Money getTotalPrice() {
        return cartLineItems.stream()
                .map(CartLineItem::getTotalCartLinePrice)
                .reduce(Money.ZERO, Money::plus);
    }
}
