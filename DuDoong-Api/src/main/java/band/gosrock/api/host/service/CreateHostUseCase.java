package band.gosrock.api.host.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.host.model.dto.request.CreateHostRequest;
import band.gosrock.api.host.model.dto.response.HostResponse;
import band.gosrock.api.host.model.mapper.HostMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostUser;
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
        final Host host = hostService.createHost(hostMapper.toEntity(createHostRequest, userId));
        // 생성한 유저를 마스터 권한으로 등록
        final HostUser masterHostUser = hostMapper.toMasterHostUser(host.getId(), userId);
        // 초대 보류 없이로 즉시 활성화
        masterHostUser.activate();
        return HostResponse.of(hostService.addHostUser(host, masterHostUser));
    }
}
