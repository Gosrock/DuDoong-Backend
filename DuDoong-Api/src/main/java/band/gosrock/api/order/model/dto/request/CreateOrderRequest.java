package band.gosrock.api.order.model.dto.request;


import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

@Getter
@RequiredArgsConstructor
public class CreateOrderRequest {
    @Nullable private final Long couponId;

    @NotNull private final Long cartId;
}
