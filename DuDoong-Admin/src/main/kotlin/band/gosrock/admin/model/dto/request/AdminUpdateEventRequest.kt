package band.gosrock.admin.model.dto.request

import java.time.LocalDateTime

data class AdminUpdateEventRequest(
    val name: String? = null,
    val startAt: LocalDateTime? = null,
    val runTime: Int? = null,
    val content: String? = null,
    val placeName: String? = null,
    val placeAddress: String? = null,
)
