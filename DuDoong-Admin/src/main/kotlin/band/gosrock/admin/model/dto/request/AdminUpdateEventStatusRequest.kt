package band.gosrock.admin.model.dto.request

import band.gosrock.domain.domains.event.domain.EventStatus

data class AdminUpdateEventStatusRequest(
    val status: EventStatus,
)
