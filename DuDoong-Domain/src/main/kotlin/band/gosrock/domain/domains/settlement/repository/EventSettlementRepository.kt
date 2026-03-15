package band.gosrock.domain.domains.settlement.repository

import band.gosrock.domain.domains.settlement.domain.EventSettlement
import org.springframework.data.repository.CrudRepository
import java.util.Optional

interface EventSettlementRepository : CrudRepository<EventSettlement, Long> {
    fun findByEventId(eventId: Long): Optional<EventSettlement>
    fun deleteByEventId(eventId: Long)
}
