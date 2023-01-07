package band.gosrock.domain.domains.cart.domain;


import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartLineId implements Serializable {

    @Column(name = "cart_line_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long value;
}
