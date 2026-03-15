package band.gosrock.api.order.model.dto.request

import band.gosrock.common.annotation.Enum
import band.gosrock.domain.domains.order.repository.condition.AdminTableOrderFilterType
import band.gosrock.domain.domains.order.repository.condition.AdminTableSearchType
import band.gosrock.domain.domains.order.repository.condition.FindEventOrdersCondition
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema

data class AdminOrderTableQueryRequest(
    @field:Enum
    val orderStage: AdminTableOrderFilterType,

    // nullable
    val searchType: AdminTableSearchType?,

    @Schema(nullable = true)
    val searchString: String?,
) {
    @JsonIgnore
    fun toCondition(eventId: Long): FindEventOrdersCondition =
        FindEventOrdersCondition(
            eventId = eventId,
            filterType = orderStage,
            searchString = searchString,
            searchType = searchType,
        )
}
