package band.gosrock.api.order.model.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

@Getter
@RequiredArgsConstructor
public class CreateOrderRequest {
    @Nullable
    @Schema(nullable = true, defaultValue = "null")
    private final Long couponId;

    @NotNull private final Long cartId;
}
