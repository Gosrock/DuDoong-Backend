package band.gosrock.domain.domains.host.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostProfile;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.host.domain.HostUser;
import band.gosrock.domain.domains.host.exception.InvalidSlackUrlException;
import band.gosrock.domain.domains.host.repository.HostRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.StringUtils;
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

    public Host addHostUser(Host host, HostUser hostUser) {
        host.addHostUsers(Set.of(hostUser));
        return hostRepository.save(host);
    }

    @RedissonLock(LockName = "호스트유저초대", identifier = "id", paramClassType = Host.class)
    public Host inviteHostUser(Host host, HostUser hostUser) {
        host.inviteHostUsers(Set.of(hostUser));
        return hostRepository.save(host);
    }

    public Host updateHostUserRole(Host host, Long userId, HostRole role) {
        host.setHostUserRole(userId, role);
        return hostRepository.save(host);
    }

    public Host updateHostProfile(Host host, HostProfile profile) {
        host.updateProfile(profile);
        return hostRepository.save(host);
    }

    public Host updateHostSlackUrl(Host host, String url) {
        host.setSlackUrl(url);
        return hostRepository.save(host);
    }

    public Host activateHostUser(Host host, Long userId) {
        host.getHostUserByUserId(userId).activate();
        return hostRepository.save(host);
    }

    public Host removeHostUser(Host host, Long userId) {
        host.removeHostUser(userId);
        return hostRepository.save(host);
    }

    public void validateDuplicatedSlackUrl(Host host, String url) {
        if (StringUtils.equals(host.getSlackUrl(), url)) {
            throw InvalidSlackUrlException.EXCEPTION;
        }
    }

    /** 해당 유저가 호스트에 속하는지 확인하는 검증 로직입니다 */
    public void validateHostUser(Host host, Long userId) {
        host.validateHostUser(userId);
    }

    /** 해당 유저가 호스트의 마스터(담당자, 방장)인지 확인하는 검증 로직입니다 */
    public void validateMasterHostUser(Host host, Long userId) {
        host.validateMasterHostUser(userId);
    }

    /** 해당 유저가 슈퍼 호스트인지 확인하는 검증 로직입니다 */
    public void validateManagerHostUser(Long hostId, Long userId) {
        Host host = hostAdaptor.findById(hostId);
        host.validateManagerHostUser(userId);
    }
}
