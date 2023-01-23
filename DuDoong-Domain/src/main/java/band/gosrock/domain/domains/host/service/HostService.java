package band.gosrock.domain.domains.host.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.host.domain.HostUser;
import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException;
import band.gosrock.domain.domains.host.exception.NotMasterHostException;
import band.gosrock.domain.domains.host.exception.NotSuperHostException;
import band.gosrock.domain.domains.host.repository.HostRepository;
import band.gosrock.domain.domains.host.repository.HostUserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HostService {
    private final HostRepository hostRepository;
    private final HostUserRepository hostUserRepository;
    private final HostAdaptor hostAdaptor;

    public Host createHost(Host host) {
        return hostRepository.save(host);
    }

    public Host save(Host host) {
        return hostRepository.save(host);
    }

    public Host addHostUser(Host host, HostUser hostUser) {
        host.addHostUsers(Set.of(hostUser));
        return hostRepository.save(host);
    }

    public Host addHostUser(Long hostId, Long userId, HostRole role) {
        Host host = hostAdaptor.findById(hostId);
        // 이 호스트에 이미 속함
        if (host.hasHostUserId(userId)) {
            throw AlreadyJoinedHostException.EXCEPTION;
        }
        HostUser hostUser = HostUser.builder().userId(userId).host(host).role(role).build();
        host.addHostUsers(Set.of(hostUser));
        return hostRepository.save(host);
    }

    public HostUser createHostUser(HostUser hostUser) {
        return hostUserRepository.save(hostUser);
    }

    /** 해당 유저가 호스트의 마스터(담당자, 방장)인지 확인하는 검증 로직입니다 */
    public void validateMasterUser(Long hostId, Long userId) {
        Host host = hostAdaptor.findById(hostId);
        if (host.getMasterUserId().equals(userId)) {
            return;
        }
        throw NotMasterHostException.EXCEPTION;
    }

    public void validateMasterUser(Host host, Long userId) {
        if (host.getMasterUserId().equals(userId)) {
            return;
        }
        throw NotMasterHostException.EXCEPTION;
    }

    /** 해당 유저가 슈퍼 호스트인지 확인하는 검증 로직입니다 */
    public void validateSuperHost(Long hostId, Long userId) {
        Host host = hostAdaptor.findById(hostId);
        if (host.isSuperHostUserId(userId)) {
            return;
        }
        throw NotSuperHostException.EXCEPTION;
    }
}
