package band.gosrock.api.host.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.api.host.model.mapper.HostMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.service.HostService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RejectHostUseCase {

    private final UserUtils userUtils;
    private final HostService hostService;
    private final HostAdaptor hostAdaptor;
    private final HostMapper hostMapper;

    @Transactional
    public HostDetailResponse execute(Long hostId) {
        final Long userId = userUtils.getCurrentUserId();
        final Host host = hostAdaptor.findById(hostId);

        return hostMapper.toHostDetailResponse(hostService.removeHostUser(host, userId));
    }
}
