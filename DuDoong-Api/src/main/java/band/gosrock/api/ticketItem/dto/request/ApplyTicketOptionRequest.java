package band.gosrock.api.ticketItem.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ApplyTicketOptionRequest {

    @NotNull
    @Schema(nullable = false, example = "1")
    private Long ticketItemId;

    @NotNull
    @Schema(nullable = false, example = "1")
    private Long optionGroupId;
}
