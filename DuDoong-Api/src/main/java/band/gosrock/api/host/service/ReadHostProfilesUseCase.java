package band.gosrock.api.host.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.common.slice.SliceParam;
import band.gosrock.api.common.slice.SliceResponse;
import band.gosrock.api.host.model.dto.response.HostProfileResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadHostProfilesUseCase {
    private final UserUtils userUtils;
    private final HostAdaptor hostAdaptor;

    public SliceResponse<HostProfileResponse> execute(SliceParam sliceParam) {
        final User user = userUtils.getCurrentUser();
        final Long userId = user.getId();

        return SliceResponse.of(
                hostAdaptor
                        .querySliceHostsByUserId(
                                userId, sliceParam.getLastId(), sliceParam.toPageable())
                        .map(host -> HostProfileResponse.of(host, userId)));
    }
}
