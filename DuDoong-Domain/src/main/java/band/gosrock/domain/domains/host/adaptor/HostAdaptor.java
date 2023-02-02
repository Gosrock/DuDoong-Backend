package band.gosrock.domain.domains.host.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.exception.HostNotFoundException;
import band.gosrock.domain.domains.host.repository.HostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

@Adaptor
@RequiredArgsConstructor
public class HostAdaptor {
    private final HostRepository hostRepository;

    public Host findById(Long hostId) {
        return hostRepository.findById(hostId).orElseThrow(() -> HostNotFoundException.EXCEPTION);
    }

    /** 자신이 속해있는 호스트 리스트를 무한스크롤로 가져오는 쿼리 요청 */
    public Slice<Host> querySliceHostsByUserId(Long userId, Long lastId, Pageable pageable) {
        return hostRepository.querySliceHostsByUserId(userId, lastId, pageable);
    }

    /** 자신이 마스터인 호스트 리스트를 가져오는 쿼리 요청 */
    public List<Host> findAllByMasterUserId(Long userId) {
        return hostRepository.findAllByMasterUserId(userId);
    }

    /** 자신이 속해있는 호스트 리스트를 가져오는 쿼리 요청 */
    public Page<Host> findAllByHostUsers_UserId(Long userId, Pageable pageable) {
        return hostRepository.findAllByHostUsers_UserId(userId, pageable);
    }

    public List<Host> findAllByHostUsers_UserId(Long userId) {
        return hostRepository.findAllByHostUsers_UserId(userId);
    }
}
