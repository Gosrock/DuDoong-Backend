package band.gosrock.api.host.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.host.model.dto.request.CreateHostRequest;
import band.gosrock.api.host.model.dto.response.CreateHostResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.host.domain.HostUser;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateHostUseCase {

    private final UserDomainService userDomainService;
    private final HostService hostService;
    private final HostAdaptor hostAdaptor;

    @Transactional
    public CreateHostResponse execute(CreateHostRequest createHostRequest) {
        Long securityUserId = SecurityUtils.getCurrentUserId();
        // 존재하는 유저인지 검증
        User user = userDomainService.retrieveUser(securityUserId);
        // 호스트 생성
        Host host = hostService.createHost(createHostRequest.toEntity(securityUserId));

        HostUser hostUser =
                HostUser.builder()
                        .userId(securityUserId)
                        .hostId(host.getId())
                        .role(HostRole.SUPER_HOST)
                        .build();

        host.addHostUsers(Collections.singletonList(hostUser));
        return CreateHostResponse.of(host);
    }
}
