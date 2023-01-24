package band.gosrock.domain.domains.host.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostProfile;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.host.domain.HostUser;
import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException;
import band.gosrock.domain.domains.host.repository.HostRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HostService {
    private final HostRepository hostRepository;
    private final HostAdaptor hostAdaptor;

    public Host createHost(Host host) {
        return hostRepository.save(host);
    }

    public Host updateHost(Host host) {
        return hostRepository.save(host);
    }

    public Host addHostUser(Host host, HostUser hostUser) {
        if (host.hasHostUserId(hostUser.getUserId())) {
            throw AlreadyJoinedHostException.EXCEPTION;
        }
        host.addHostUsers(Set.of(hostUser));
        return hostRepository.save(host);
    }

    public Host updateHostUserRole(Host host, Long userId, HostRole role) {
        host.setHostUserRole(userId, role);
        return hostRepository.save(host);
    }

    public Host updateHostProfile(Host host, HostProfile profile) {
        host.setProfile(profile);
        return hostRepository.save(host);
    }

    public Host updateHostSlackUrl(Host host, String url) {
        host.setSlackUrl(url);
        return hostRepository.save(host);
    }

    /** 해당 유저가 호스트의 마스터(담당자, 방장)인지 확인하는 검증 로직입니다 */
    public void validateMasterHostUser(Host host, Long userId) {
        host.validateMasterHostUser(userId);
    }

    /** 해당 유저가 슈퍼 호스트인지 확인하는 검증 로직입니다 */
    public void validateSuperHostUser(Long hostId, Long userId) {
        Host host = hostAdaptor.findById(hostId);
        host.validateSuperHostUser(userId);
    }
}
