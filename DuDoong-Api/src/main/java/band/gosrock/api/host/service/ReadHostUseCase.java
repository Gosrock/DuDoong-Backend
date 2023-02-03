package band.gosrock.api.host.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.api.host.model.mapper.HostMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReadHostUseCase {
    private final UserUtils userUtils;
    private final HostMapper hostMapper;

    @Transactional(readOnly = true)
    public HostDetailResponse execute(Long hostId) {
        final User user = userUtils.getCurrentUser();
        final Long userId = user.getId();

        return hostMapper.toHostDetailResponse(hostId);
    }
}
