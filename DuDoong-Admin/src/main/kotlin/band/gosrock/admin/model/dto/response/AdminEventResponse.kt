package band.gosrock.admin.model.dto.response

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
    val ticketItemCount: Int = 0,
    val issuedTicketCount: Int = 0,
    val totalOrderCount: Int = 0,
    val content: String? = null,
    val placeName: String? = null,
    val placeAddress: String? = null,
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

        fun ofDetail(
            event: Event,
            hostName: String?,
            ticketItemCount: Int,
            issuedTicketCount: Int,
            totalOrderCount: Int,
        ): AdminEventResponse =
            AdminEventResponse(
                id = event.id!!,
                name = event.eventBasic?.name,
                hostName = hostName,
                status = event.status,
                startAt = event.eventBasic?.startAt,
                runTime = event.eventBasic?.runTime,
                createdAt = event.createdAt,
                ticketItemCount = ticketItemCount,
                issuedTicketCount = issuedTicketCount,
                totalOrderCount = totalOrderCount,
                content = event.eventDetail?.content,
                placeName = event.eventPlace?.placeName,
                placeAddress = event.eventPlace?.placeAddress,
            )
    }
}
