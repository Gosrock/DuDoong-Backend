package band.gosrock.domain.domains.settlement.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.settlement.domain.TransactionSettlement;
import band.gosrock.domain.domains.settlement.repository.TransactionSettlementRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class TransactionSettlementAdaptor {

    private final TransactionSettlementRepository transactionSettlementRepository;

    public void saveAll(List<TransactionSettlement> transactionSettlements) {
        transactionSettlementRepository.saveAll(transactionSettlements);
    }

    public List<TransactionSettlement> findByEventId(Long eventId) {
        return transactionSettlementRepository.findByEventId(eventId);
    }
}
