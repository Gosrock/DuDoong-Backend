package band.gosrock.domain.domains.host.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.host.domain.HostUser;
import band.gosrock.domain.domains.host.exception.HostNotFoundException;
import band.gosrock.domain.domains.host.exception.NotSuperHostException;
import band.gosrock.domain.domains.host.repository.HostRepository;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HostService {
    private final HostRepository hostRepository;

    public Host createHost(Host host) {
        return hostRepository.save(host);
    }

    public Host addHostUser(Host host, HostUser hostUser) {
        host.addHostUsers(Collections.singletonList(hostUser));
        return hostRepository.save(host);
    }

    public Host addHostUser(Host host, Long userId) {
        HostUser hostUser =
                HostUser.builder()
                        .userId(userId)
                        .hostId(host.getId())
                        .role(HostRole.SUPER_HOST)
                        .build();
        host.addHostUsers(Collections.singletonList(hostUser));
        return hostRepository.save(host);
    }

    /** 해당 유저가 호스트의 마스터(담당자, 방장)인지 확인하는 검증 로직입니다 */
    public void checkSuperHost(Long hostId, Long userId) {
        Host host =
                hostRepository.findById(hostId).orElseThrow(() -> HostNotFoundException.EXCEPTION);
        if (!host.getMasterUserId().equals(userId)) {
            throw NotSuperHostException.EXCEPTION;
        }
    }
}
