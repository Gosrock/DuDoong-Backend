package band.gosrock.api.host.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.host.model.dto.request.CreateHostRequest;
import band.gosrock.api.host.model.dto.response.HostResponse;
import band.gosrock.api.host.model.mapper.HostMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateHostUseCase {

    private final UserDomainService userDomainService;
    private final HostService hostService;
    private final HostMapper hostMapper;

    @Transactional
    public HostResponse execute(CreateHostRequest createHostRequest) {
        final Long userId = SecurityUtils.getCurrentUserId();
        // 존재하는 유저인지 검증
        final User user = userDomainService.retrieveUser(userId);
        // 호스트 생성
        final Host host = hostMapper.toEntity(createHostRequest, userId);
        hostService.createHost(host);
        return HostResponse.of(hostService.addHostUser(host, userId, HostRole.SUPER_HOST));
    }
}
