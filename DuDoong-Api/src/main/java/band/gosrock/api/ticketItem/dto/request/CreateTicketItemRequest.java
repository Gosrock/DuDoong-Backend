package band.gosrock.api.ticketItem.dto.request;


import band.gosrock.common.annotation.Enum;
import band.gosrock.domain.domains.ticket_item.domain.TicketType;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

@Getter
@RequiredArgsConstructor
public class CreateTicketItemRequest {

    @Schema(nullable = false, defaultValue = "선착순")
    @Enum(message = "선착순, 승인만 허용됩니다")
    private TicketType type;

    @NotEmpty(message = "티켓상품 이름을 입력해주세요")
    @Schema(nullable = false, example = "일반 티켓")
    private String name;

    @Nullable
    @Schema(nullable = true, example = "일반 입장 티켓입니다.")
    private String description;

    @NotNull
    @Schema(defaultValue = "0", nullable = false, example = "4000")
    private Long price;

    @NotNull
    @Schema(nullable = false, example = "100")
    private Long supplyCount;

    @NotNull
    @Schema(nullable = false, example = "1")
    private Long purchaseLimit;
}
