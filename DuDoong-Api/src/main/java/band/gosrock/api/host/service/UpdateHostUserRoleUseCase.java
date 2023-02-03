package band.gosrock.api.host.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER;

import band.gosrock.api.common.UserUtils;
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.host.model.dto.request.UpdateHostUserRoleRequest;
import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.api.host.model.mapper.HostMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.host.service.HostService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateHostUserRoleUseCase {
    private final UserUtils userUtils;
    private final HostService hostService;
    private final HostAdaptor hostAdaptor;
    private final HostMapper hostMapper;

    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    public HostDetailResponse execute(
            Long hostId, UpdateHostUserRoleRequest updateHostUserRoleRequest) {
        final Host host = hostAdaptor.findById(hostId);
        final Long updateUserId = updateHostUserRoleRequest.getUserId();
        final HostRole updateUserRole = updateHostUserRoleRequest.getRole();

        return hostMapper.toHostDetailResponse(
                hostService.updateHostUserRole(host, updateUserId, updateUserRole));
    }
}
