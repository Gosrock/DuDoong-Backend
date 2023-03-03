package band.gosrock.domain.domains.settlement.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.settlement.adaptor.EventSettlementAdaptor;
import band.gosrock.domain.domains.settlement.domain.EventSettlement;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@RequiredArgsConstructor
public class EventSettlementDomainService {

    private final EventSettlementAdaptor eventSettlementAdaptor;

    @Transactional
    public void updateEventOrderListExcelFileKey(Long eventId, String key) {
        EventSettlement eventSettlement = eventSettlementAdaptor.upsertByEventId(eventId);
        eventSettlement.updateEventOrderListExcelKey(key);
    }
}
