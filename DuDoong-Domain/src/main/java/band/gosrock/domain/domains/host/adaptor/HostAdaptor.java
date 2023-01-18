package band.gosrock.domain.domains.host.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.exception.HostNotFoundException;
import band.gosrock.domain.domains.host.repository.HostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class HostAdaptor {
    private final HostRepository hostRepository;

    public Host findById(Long hostId) {
        return hostRepository.findById(hostId).orElseThrow(() -> HostNotFoundException.EXCEPTION);
    }

    public List<Host> findAllByMasterUserId(Long userId) {
        return hostRepository.findAllByMasterUserId(userId);
    }

    //    public List<Host> findAllByUserId(Long userId) {
    //        return hostRepository.findAllByMasterUserId(userId);
    ////                .orElseThrow(() -> HostNotFoundException.EXCEPTION);
    //    }

}
