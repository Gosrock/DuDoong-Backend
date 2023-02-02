package band.gosrock.domain.domains.host.repository;


import band.gosrock.domain.domains.host.domain.Host;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

public interface HostRepository extends CrudRepository<Host, Long>, HostCustomRepository {
    List<Host> findAllByMasterUserId(Long userId);

    List<Host> findAllByHostUsers_UserId(Long userId);

    Page<Host> findAllByHostUsers_UserId(Long userId, Pageable pageable);
}
