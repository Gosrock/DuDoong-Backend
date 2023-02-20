package band.gosrock.domain.domains.settlement.repository;


import band.gosrock.domain.domains.settlement.domain.TransactionSettlement;
import org.springframework.data.repository.CrudRepository;

public interface TransactionSettlementRepository
        extends CrudRepository<TransactionSettlement, Long> {}
