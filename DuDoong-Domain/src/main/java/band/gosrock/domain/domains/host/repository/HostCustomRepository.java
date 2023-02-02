package band.gosrock.domain.domains.host.repository;


import band.gosrock.domain.domains.host.domain.Host;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface HostCustomRepository {
    Slice<Host> querySliceHostsByUserId(Long id, Long lastId, Pageable pageable);
}
