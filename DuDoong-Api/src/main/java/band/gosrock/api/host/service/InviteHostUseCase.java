package band.gosrock.api.host.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.host.model.dto.request.InviteHostRequest;
import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.api.host.model.mapper.HostMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostUser;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class InviteHostUseCase {

    private final UserAdaptor userAdaptor;
    private final HostService hostService;
    private final HostAdaptor hostAdaptor;
    private final HostMapper hostMapper;

    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    public HostDetailResponse execute(Long hostId, InviteHostRequest inviteHostRequest) {
        // 초대할 유저
        final Long inviteUserId =
                userAdaptor.queryUserByEmail(inviteHostRequest.getEmail()).getId();
        final Host host = hostAdaptor.findById(hostId);
        final HostUser hostUser =
                hostMapper.toHostUser(hostId, inviteUserId, inviteHostRequest.getRole());
        return hostMapper.toHostDetailResponse(hostService.addHostUser(host, hostUser));

        // todo :: 초대 성공 시 상대방에게 알림 이벤트 발송
    }
}
