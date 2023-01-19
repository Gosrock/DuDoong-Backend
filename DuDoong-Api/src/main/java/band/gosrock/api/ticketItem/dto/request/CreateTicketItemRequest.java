package band.gosrock.api.ticketItem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;

@Getter
@RequiredArgsConstructor
public class CreateTicketItemRequest {

    @NotNull
    @Schema(nullable = false, example = "APPROVAL")
    private String type;

    @NotNull
    @Schema(nullable = false, example = "일반 티켓")
    private String name;

    @Nullable
    @Schema(nullable = true, example = "일반 입장 티켓입니다.")
    private String description;

    @NotNull
    @Schema(nullable = false, example = "4000")
    private Long price;

    @NotNull
    @Schema(nullable = false, example = "100")
    private Long supplyCount;

    @NotNull
    @Schema(nullable = false, example = "1")
    private Long purchaseLimit;

}
