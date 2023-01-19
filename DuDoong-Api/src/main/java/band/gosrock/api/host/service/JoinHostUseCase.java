package band.gosrock.api.host.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class JoinHostUseCase {

    private final UserDomainService userDomainService;
    private final HostService hostService;
    private final HostAdaptor hostAdaptor;

    @Transactional
    public HostDetailResponse execute(Long hostId) {
        final Long securityUserId = SecurityUtils.getCurrentUserId();
        final User user = userDomainService.retrieveUser(securityUserId);
        final Host host = hostAdaptor.findById(hostId);

        // 이 호스트에 이미 속함
        if (hostService.hasHostUser(host, securityUserId)) {
            throw AlreadyJoinedHostException.EXCEPTION;
        }
        return HostDetailResponse.of(hostService.addHostUser(host, securityUserId, HostRole.HOST));
    }
}
