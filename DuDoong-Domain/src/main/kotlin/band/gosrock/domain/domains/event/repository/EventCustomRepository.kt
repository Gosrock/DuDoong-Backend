package band.gosrock.domain.domains.event.repository

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import java.time.LocalDateTime
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface EventCustomRepository {
    fun querySliceEventsByHostIdIn(hostIds: List<Long>, pageable: Pageable): Slice<Event>

    fun querySliceEventsByStatus(status: EventStatus, pageable: Pageable): Slice<Event>

    fun querySliceEventsByKeyword(keyword: String, pageable: Pageable): Slice<Event>

    fun queryEventsByEndAtBeforeAndStatusOpen(time: LocalDateTime): List<Event>
}
