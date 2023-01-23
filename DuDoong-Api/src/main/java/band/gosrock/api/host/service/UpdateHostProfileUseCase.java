package band.gosrock.api.host.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.host.model.dto.request.UpdateHostRequest;
import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.api.host.model.mapper.HostMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateHostProfileUseCase {
    private final UserUtils userUtils;
    private final HostService hostService;
    private final HostAdaptor hostAdaptor;
    private final HostMapper hostMapper;

    @Transactional
    public HostDetailResponse execute(Long hostId, UpdateHostRequest updateHostRequest) {
        // 존재하는 유저인지 검증
        final User user = userUtils.getCurrentUser();
        final Long userId = user.getId();

        final Host host = hostAdaptor.findById(hostId);

        // 마스터 호스트 검증
        hostService.validateMasterUser(host, userId);
        host.setProfile(hostMapper.toHostProfile(updateHostRequest));
        return hostMapper.toHostDetailResponse(hostService.updateHost(host));
    }
}
