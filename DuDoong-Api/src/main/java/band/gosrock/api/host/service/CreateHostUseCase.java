package band.gosrock.api.host.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.host.model.dto.request.CreateHostRequest;
import band.gosrock.api.host.model.dto.response.HostResponse;
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

    @Transactional
    public HostResponse execute(CreateHostRequest createHostRequest) {
        final Long securityUserId = SecurityUtils.getCurrentUserId();
        // 존재하는 유저인지 검증
        final User user = userDomainService.retrieveUser(securityUserId);
        // 호스트 생성
        final Host host =
                hostService.createHost(
                        Host.builder()
                                .contactEmail(createHostRequest.getContactEmail())
                                .contactNumber(createHostRequest.getContactNumber())
                                .masterUserId(securityUserId)
                                .build());
        return HostResponse.of(hostService.addHostUser(host, securityUserId, HostRole.SUPER_HOST));
    }
}
