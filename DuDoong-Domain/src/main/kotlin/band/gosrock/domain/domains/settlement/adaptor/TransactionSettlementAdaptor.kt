package band.gosrock.domain.domains.settlement.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.settlement.domain.TransactionSettlement
import band.gosrock.domain.domains.settlement.repository.TransactionSettlementRepository

@Adaptor
class TransactionSettlementAdaptor(
    private val transactionSettlementRepository: TransactionSettlementRepository,
) {
    fun saveAll(transactionSettlements: List<TransactionSettlement>) {
        transactionSettlementRepository.saveAll(transactionSettlements)
    }

    fun findByEventId(eventId: Long): List<TransactionSettlement> =
        transactionSettlementRepository.findByEventId(eventId)

    fun deleteByEventId(eventId: Long) =
        transactionSettlementRepository.deleteByEventId(eventId)
}
