package band.gosrock.api.host.service;


import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.api.host.model.mapper.HostMapper;
import band.gosrock.common.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReadHostUseCase {
    private final HostMapper hostMapper;

    @Transactional(readOnly = true)
    public HostDetailResponse execute(Long hostId) {
        return hostMapper.toHostDetailResponse(hostId);
    }
}
