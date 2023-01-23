package band.gosrock.api.host.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.host.model.dto.request.UpdateHostUserRoleRequest;
import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.api.host.model.mapper.HostMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.host.exception.ForbiddenHostOperationException;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.user.domain.User;
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
    public HostDetailResponse execute(
            Long hostId, UpdateHostUserRoleRequest updateHostUserRoleRequest) {
        // 존재하는 유저인지 검증
        final User user = userUtils.getCurrentUser();
        final Host host = hostAdaptor.findById(hostId);
        final Long userId = user.getId();

        final Long updateUserId = updateHostUserRoleRequest.getUserId();
        final HostRole updateUserRole = updateHostUserRoleRequest.getRole();

        // 마스터 호스트 검증
        hostService.validateSuperHost(host, userId);

        // 마스터 호스트의 권한은 변경할 수 없음
        if (host.getMasterUserId().equals(updateUserId)) {
            throw ForbiddenHostOperationException.EXCEPTION;
        }

        return hostMapper.toHostDetailResponse(
                hostService.updateHostUserRole(host, updateUserId, updateUserRole));
    }
}
