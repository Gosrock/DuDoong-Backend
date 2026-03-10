package band.gosrock.domain.domains.event.repository

import band.gosrock.domain.domains.event.domain.Event
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository

interface EventRepository : CrudRepository<Event, Long>, EventCustomRepository {
    override fun findAll(): List<Event>

    fun findAllByHostId(hostId: Long, pageable: Pageable): Page<Event>

    fun findAllByIdIn(ids: List<Long>): List<Event>

    fun findAllByHostIdIn(hostIds: List<Long>, pageable: Pageable): Page<Event>
}
