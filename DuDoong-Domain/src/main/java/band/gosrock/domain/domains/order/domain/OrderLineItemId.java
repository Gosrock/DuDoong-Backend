package band.gosrock.domain.domains.order.domain;


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
public class OrderLineItemId implements Serializable {

    @Column(name = "order_line_item_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long value;
}
