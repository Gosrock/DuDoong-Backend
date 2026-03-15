package band.gosrock.domain.domains.settlement.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.settlement.domain.EventSettlement
import band.gosrock.domain.domains.settlement.repository.EventSettlementRepository

@Adaptor
class EventSettlementAdaptor(
    private val eventSettlementRepository: EventSettlementRepository,
) {
    fun save(eventSettlement: EventSettlement): EventSettlement =
        eventSettlementRepository.save(eventSettlement)

    fun findByEventId(eventId: Long): EventSettlement =
        eventSettlementRepository.findByEventId(eventId).orElseThrow()

    fun upsertByEventId(eventId: Long): EventSettlement =
        eventSettlementRepository.findByEventId(eventId)
            .orElseGet { eventSettlementRepository.save(EventSettlement.createWithEventId(eventId)) }

    fun deleteByEventId(eventId: Long) =
        eventSettlementRepository.deleteByEventId(eventId)
}
