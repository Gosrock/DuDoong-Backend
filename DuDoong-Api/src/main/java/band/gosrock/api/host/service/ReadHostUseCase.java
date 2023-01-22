package band.gosrock.api.host.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.api.host.model.mapper.HostMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadHostUseCase {
    private final UserDomainService userDomainService;
    private final HostMapper hostMapper;

    public HostDetailResponse execute(Long hostId) {
        final Long securityUserId = SecurityUtils.getCurrentUserId();
        final User user = userDomainService.retrieveUser(securityUserId);
        return hostMapper.toHostDetailResponse(hostId);
    }
}
