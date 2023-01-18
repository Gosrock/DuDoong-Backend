package band.gosrock.api.host.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostUser;
import band.gosrock.domain.domains.host.exception.ForbiddenHostException;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadHostUseCase {
    private final UserDomainService userDomainService;
    private final HostAdaptor hostAdaptor;

    public HostDetailResponse execute(Long hostId) {
        final Long securityUserId = SecurityUtils.getCurrentUserId();
        final User user = userDomainService.retrieveUser(securityUserId);
        final Host host = hostAdaptor.findById(hostId);
        for (HostUser hostUser : host.getHostUsers()) {
            // 이 호스트에 속한 유저에게 접근 인가
            if (hostUser.getUserId().equals(securityUserId)) {
                return HostDetailResponse.of(host);
            }
        }
        // 이 호스트에 대한 접근 권한이 없음
        throw ForbiddenHostException.EXCEPTION;
    }
}