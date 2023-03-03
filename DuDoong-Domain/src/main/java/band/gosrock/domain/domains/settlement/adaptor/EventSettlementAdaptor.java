package band.gosrock.domain.domains.settlement.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.settlement.domain.EventSettlement;
import band.gosrock.domain.domains.settlement.repository.EventSettlementRepository;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class EventSettlementAdaptor {

    private final EventSettlementRepository eventSettlementRepository;

    public EventSettlement save(EventSettlement eventSettlement) {
        return eventSettlementRepository.save(eventSettlement);
    }

    public EventSettlement findByEventId(Long eventId) {
        return eventSettlementRepository.findByEventId(eventId).orElseThrow();
    }

    public EventSettlement upsertByEventId(Long eventId) {
        return eventSettlementRepository
                .findByEventId(eventId)
                .orElseGet(
                        () ->
                                eventSettlementRepository.save(
                                        EventSettlement.createWithEventId(eventId)));
    }

    public void deleteByEventId(Long eventId) {
        eventSettlementRepository.deleteByEventId(eventId);
    }
}
