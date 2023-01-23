package band.gosrock.api.host.model.mapper;


import band.gosrock.api.host.model.dto.request.CreateHostRequest;
import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.common.vo.UserInfoVo;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class HostMapper {
    private final HostAdaptor hostAdaptor;
    private final UserAdaptor userAdaptor;

    @Transactional(readOnly = true)
    public Host toEntity(CreateHostRequest createHostRequest, Long masterUserId) {
        return Host.builder()
                .contactEmail(createHostRequest.getContactEmail())
                .contactNumber(createHostRequest.getContactNumber())
                .masterUserId(masterUserId)
                .build();
    }

    @Transactional(readOnly = true)
    public HostDetailResponse toHostDetailResponse(Long hostId) {
        final Host host = hostAdaptor.findById(hostId);
        return toHostDetailResponseExecute(host);
    }

    @Transactional(readOnly = true)
    public HostDetailResponse toHostDetailResponse(Host host) {
        return toHostDetailResponseExecute(host);
    }

    private HostDetailResponse toHostDetailResponseExecute(Host host) {
        Set<Long> userIdList = new HashSet<>();

        host.getHostUsers().forEach(hostUser -> userIdList.add(hostUser.getHostId()));
        final Set<UserInfoVo> userInfoVoSet =
                userAdaptor.queryUserListByIdIn(userIdList).stream()
                        .map(User::toUserInfoVo)
                        .collect(Collectors.toSet());

        return HostDetailResponse.of(host, userInfoVoSet);
    }
}
