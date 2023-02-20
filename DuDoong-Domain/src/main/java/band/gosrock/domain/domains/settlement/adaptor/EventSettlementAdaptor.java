package band.gosrock.domain.domains.settlement.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.settlement.domain.EventSettlement;
import band.gosrock.domain.domains.settlement.repository.EventSettlementRepository;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class EventSettlementAdaptor {

    private final EventSettlementRepository eventSettlementRepository;

    public EventSettlement upsertByEventId(EventSettlement eventSettlement) {
        return eventSettlementRepository
                .findByEventId(eventSettlement.getEventId())
                .orElseGet(() -> eventSettlementRepository.save(eventSettlement));
    }
}
