package band.gosrock.api.admin.model.dto.response

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import java.time.LocalDateTime

data class AdminEventResponse(
    val id: Long,
    val name: String?,
    val hostName: String?,
    val status: EventStatus,
    val startAt: LocalDateTime?,
    val runTime: Long?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun of(event: Event, hostName: String?): AdminEventResponse =
            AdminEventResponse(
                id = event.id!!,
                name = event.eventBasic?.name,
                hostName = hostName,
                status = event.status,
                startAt = event.eventBasic?.startAt,
                runTime = event.eventBasic?.runTime,
                createdAt = event.createdAt,
            )
    }
}
