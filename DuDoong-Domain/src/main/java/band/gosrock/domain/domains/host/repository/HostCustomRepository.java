package band.gosrock.domain.domains.host.repository;


import band.gosrock.domain.domains.host.domain.Host;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface HostCustomRepository {
    Slice<Host> querySliceHostsByUserId(Long id, Pageable pageable);

    List<Host> queryHostsByActiveUserId(Long id);

    Slice<Host> querySliceHostsByActiveUserId(Long id, Pageable pageable);
}
