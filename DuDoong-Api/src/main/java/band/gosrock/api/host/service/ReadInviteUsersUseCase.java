package band.gosrock.api.host.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.GUEST;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.host.model.mapper.HostMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.common.vo.UserProfileVo;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReadInviteUsersUseCase {
    private final HostMapper hostMapper;

    @Transactional(readOnly = true)
    @HostRolesAllowed(role = GUEST, findHostFrom = HOST_ID)
    public UserProfileVo execute(Long hostId, String email) {
        return hostMapper.toHostInviteUserList(hostId, email);
    }
}
