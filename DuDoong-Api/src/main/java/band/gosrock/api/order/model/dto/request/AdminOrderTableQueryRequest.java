package band.gosrock.api.order.model.dto.request;


import band.gosrock.common.annotation.Enum;
import band.gosrock.domain.domains.order.repository.condition.AdminTableOrderFilterType;
import band.gosrock.domain.domains.order.repository.condition.AdminTableSearchType;
import band.gosrock.domain.domains.order.repository.condition.FindEventOrdersCondition;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminOrderTableQueryRequest {

    @Enum private AdminTableOrderFilterType orderStage;

    // nullable
    private AdminTableSearchType searchType;

    @Schema(nullable = true)
    private String searchString;

    @JsonIgnore
    public FindEventOrdersCondition toCondition(Long eventId) {
        return FindEventOrdersCondition.builder()
                .eventId(eventId)
                .filterType(orderStage)
                .searchString(searchString)
                .searchType(searchType)
                .build();
    }
}
