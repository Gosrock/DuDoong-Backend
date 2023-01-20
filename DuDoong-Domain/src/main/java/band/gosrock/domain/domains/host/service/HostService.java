package band.gosrock.domain.domains.host.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.host.domain.HostUser;
import band.gosrock.domain.domains.host.exception.HostNotFoundException;
import band.gosrock.domain.domains.host.exception.NotMasterHostException;
import band.gosrock.domain.domains.host.exception.NotSuperHostException;
import band.gosrock.domain.domains.host.repository.HostRepository;
import java.util.Set;
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
        host.addHostUsers(Set.of(hostUser));
        return hostRepository.save(host);
    }

    public Host addHostUser(Host host, Long userId, HostRole role) {
        HostUser hostUser =
                HostUser.builder().userId(userId).hostId(host.getId()).role(role).build();
        host.addHostUsers(Set.of(hostUser));
        return hostRepository.save(host);
    }

    /** 해당 호스트 유저에 특정 userId 가 포함되어 있는지 반환하는 로직입니다 */
    public Boolean hasHostUser(Host host, Long userId) {
        return host.getHostUsers().stream()
                .anyMatch(hostUser -> hostUser.getUserId().equals(userId));
    }

    /** 해당 유저가 호스트의 마스터(담당자, 방장)인지 확인하는 메소드입니다 */
    public Boolean isMasterUser(Long hostId, Long userId) {
        Host host =
                hostRepository.findById(hostId).orElseThrow(() -> HostNotFoundException.EXCEPTION);
        if (!host.getMasterUserId().equals(userId)) {
            return false;
        }
        return true;
    }

    /** 해당 유저가 호스트의 마스터(담당자, 방장)인지 확인하는 검증 로직입니다 */
    public void validateMasterUser(Long hostId, Long userId) {
        if (isMasterUser(hostId, userId)) {
            throw NotMasterHostException.EXCEPTION;
        }
    }

    /** 해당 유저가 슈퍼호스트인지 확인하는 메소드입니다 */
    public Boolean isSuperHost(Long hostId, Long userId) {
        Host host =
                hostRepository.findById(hostId).orElseThrow(() -> HostNotFoundException.EXCEPTION);
        return host.getHostUsers().stream()
                .anyMatch(
                        hostUser ->
                                hostUser.getUserId().equals(userId)
                                        && hostUser.getRole().equals(HostRole.SUPER_HOST));
    }

    /** 해당 유저가 호스트의 마스터(담당자, 방장)인지 확인하는 검증 로직입니다 */
    public void validateSuperHost(Long hostId, Long userId) {
        if (isSuperHost(hostId, userId)) {
            throw NotSuperHostException.EXCEPTION;
        }
    }
}
