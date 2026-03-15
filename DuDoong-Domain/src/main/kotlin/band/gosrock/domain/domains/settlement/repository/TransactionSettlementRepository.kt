package band.gosrock.domain.domains.settlement.repository

import band.gosrock.domain.domains.settlement.domain.TransactionSettlement
import org.springframework.data.repository.CrudRepository

interface TransactionSettlementRepository : CrudRepository<TransactionSettlement, Long> {
    fun findByEventId(eventId: Long): List<TransactionSettlement>
    fun deleteByEventId(eventId: Long)
}
