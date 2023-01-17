package band.gosrock.domain.domains.host.repository;


import band.gosrock.domain.domains.host.domain.Host;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface HostRepository extends CrudRepository<Host, Long> {
    List<Host> findAllByMasterUserId(Long userId);

}
