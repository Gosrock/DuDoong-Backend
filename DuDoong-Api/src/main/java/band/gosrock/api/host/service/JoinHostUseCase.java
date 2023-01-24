package band.gosrock.api.host.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.api.host.model.mapper.HostMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class JoinHostUseCase {

    private final UserUtils userUtils;
    private final HostService hostService;
    private final HostAdaptor hostAdaptor;
    private final HostMapper hostMapper;

    @Transactional
    public HostDetailResponse execute(Long hostId) {
        // 존재하는 유저인지 검증
        final User user = userUtils.getCurrentUser();
        final Long userId = user.getId();
        final Host host = hostAdaptor.findById(hostId);

        // 이 호스트에 이미 속함
        if (host.hasHostUserId(userId)) {
            throw AlreadyJoinedHostException.EXCEPTION;
        }
        hostService.addHostUser(host, hostMapper.toHostUser(hostId, userId));
        return hostMapper.toHostDetailResponse(host);
    }
}
