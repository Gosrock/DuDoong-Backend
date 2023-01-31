package band.gosrock.api.order.model.dto.request;

import band.gosrock.common.annotation.Enum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminOrderTableQueryRequest {
    @Enum
    private OrderStage orderStage;

    // nullable
    private AdminTableSearchType searchType;

    @Schema(nullable = true)
    private String searchString;
}
