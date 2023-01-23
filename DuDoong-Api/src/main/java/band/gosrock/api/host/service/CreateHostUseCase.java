package band.gosrock.api.host.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.host.model.dto.request.CreateHostRequest;
import band.gosrock.api.host.model.dto.response.HostResponse;
import band.gosrock.api.host.model.mapper.HostMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateHostUseCase {
    private final UserUtils userUtils;
    private final HostService hostService;
    private final HostMapper hostMapper;

    @Transactional
    public HostResponse execute(CreateHostRequest createHostRequest) {
        // 존재하는 유저인지 검증
        final User user = userUtils.getCurrentUser();
        final Long userId = user.getId();
        // 호스트 생성
        final Host host = hostMapper.toEntity(createHostRequest, userId);
        hostService.createHost(host);

        return HostResponse.of(
                hostService.addHostUser(host, hostMapper.toSuperHostUser(host.getId(), userId)));
    }
}
