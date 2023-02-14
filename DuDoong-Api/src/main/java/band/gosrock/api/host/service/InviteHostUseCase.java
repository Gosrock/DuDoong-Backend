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
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.host.domain.HostUser;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
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
        final User user = userAdaptor.queryUserByEmail(inviteHostRequest.getEmail());
        final Long inviteUserId = user.getId();
        final Host host = hostAdaptor.findById(hostId);
        final HostRole role = inviteHostRequest.getRole();
        final HostUser hostUser = hostMapper.toHostUser(hostId, inviteUserId, role);

        return hostMapper.toHostDetailResponse(hostService.inviteHostUser(host, hostUser));
    }
}
