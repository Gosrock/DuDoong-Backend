package band.gosrock.api.order.model.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class CreateOrderRequest {
    @Nullable
    @Schema(nullable = true, defaultValue = "null")
    private Long couponId;

    @NotNull private Long cartId;
}
