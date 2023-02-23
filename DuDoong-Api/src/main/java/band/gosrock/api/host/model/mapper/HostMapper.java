package band.gosrock.api.host.model.mapper;


import band.gosrock.api.host.model.dto.request.CreateHostRequest;
import band.gosrock.api.host.model.dto.request.UpdateHostRequest;
import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.common.vo.HostUserVo;
import band.gosrock.domain.common.vo.UserInfoVo;
import band.gosrock.domain.common.vo.UserProfileVo;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostProfile;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.host.domain.HostUser;
import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostMapper {
    private final HostAdaptor hostAdaptor;
    private final UserAdaptor userAdaptor;

    public Host toEntity(CreateHostRequest createHostRequest, Long masterUserId) {
        return Host.builder()
                .name(createHostRequest.getName())
                .contactEmail(createHostRequest.getContactEmail())
                .contactNumber(createHostRequest.getContactNumber())
                .masterUserId(masterUserId)
                .build();
    }

    public HostProfile toHostProfile(UpdateHostRequest updateHostRequest) {
        return HostProfile.builder()
                .introduce(updateHostRequest.getIntroduce())
                .profileImageKey(updateHostRequest.getProfileImageKey())
                .contactEmail(updateHostRequest.getContactEmail())
                .contactNumber(updateHostRequest.getContactNumber())
                .build();
    }

    /** 호스트 역할을 지정하여 주입하는 생성자 */
    public HostUser toHostUser(Long hostId, Long userId, HostRole role) {
        final Host host = hostAdaptor.findById(hostId);
        return HostUser.builder().userId(userId).host(host).role(role).build();
    }

    /** 매니저로 주입하는 생성자 */
    public HostUser toManagerHostUser(Long hostId, Long userId) {
        final Host host = hostAdaptor.findById(hostId);
        return HostUser.builder().userId(userId).host(host).role(HostRole.MANAGER).build();
    }

    /** 마스터 주입하는 생성자 */
    public HostUser toMasterHostUser(Long hostId, Long userId) {
        final Host host = hostAdaptor.findById(hostId);
        return HostUser.builder().userId(userId).host(host).role(HostRole.MASTER).build();
    }

    public UserProfileVo toHostInviteUserList(Long hostId, String email) {
        final Host host = hostAdaptor.findById(hostId);

        final User inviteUser = userAdaptor.queryUserByEmail(email);
        if (host.hasHostUserId(inviteUser.getId())) {
            throw AlreadyJoinedHostException.EXCEPTION;
        }
        return inviteUser.toUserProfileVo();
    }

    public HostDetailResponse toHostDetailResponse(Long hostId) {
        final Host host = hostAdaptor.findById(hostId);
        return toHostDetailResponseExecute(host);
    }

    public HostDetailResponse toHostDetailResponse(Host host) {
        return toHostDetailResponseExecute(host);
    }

    private HostDetailResponse toHostDetailResponseExecute(Host host) {
        final List<Long> userIds = host.getHostUser_UserIds();
        final List<User> userList = userAdaptor.queryUserListByIdIn(userIds);
        final Map<Long, User> userMap =
                userList.stream().collect(Collectors.toMap(User::getId, user -> user));
        final List<HostUserVo> hostUserVoList = new ArrayList<>();

        for (Long userId : userIds) {
            final User user = userMap.get(userId);
            if (user != null) {
                final UserInfoVo userInfoVo = user.toUserInfoVo();
                final HostUser hostUser = host.getHostUserByUserId(userId);
                final HostUserVo hostUserVo = HostUserVo.from(userInfoVo, hostUser);
                hostUserVoList.add(hostUserVo);
            }
        }

        return HostDetailResponse.of(host, hostUserVoList);
    }
}
