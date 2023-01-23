package band.gosrock.api.host.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.host.model.dto.request.InviteHostRequest;
import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.api.host.model.mapper.HostMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostUser;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class InviteHostUseCase {

    private final UserUtils userUtils;
    private final UserAdaptor userAdaptor;
    private final HostService hostService;
    private final HostAdaptor hostAdaptor;
    private final HostMapper hostMapper;

    @Transactional
    public HostDetailResponse execute(Long hostId, InviteHostRequest inviteHostRequest) {
        // 존재하는 유저인지 검증
        final User user = userUtils.getCurrentUser();
        // 초대할 유저
        final Long inviteUserId =
                userAdaptor.queryUserByEmail(inviteHostRequest.getEmail()).getId();
        final Host host = hostAdaptor.findById(hostId);
        final HostUser hostUser = hostMapper.toHostUser(hostId, inviteUserId);
        return hostMapper.toHostDetailResponse(hostService.addHostUser(host, hostUser));
    }
}
