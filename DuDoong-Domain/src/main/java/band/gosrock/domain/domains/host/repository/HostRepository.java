package band.gosrock.domain.domains.host.repository;


import band.gosrock.domain.domains.host.domain.Host;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface HostRepository extends CrudRepository<Host, Long> {
    List<Host> findAllByMasterUserId(Long userId);
}
