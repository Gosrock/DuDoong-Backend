package band.gosrock.api.event.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.common.slice.SliceParam;
import band.gosrock.api.common.slice.SliceResponse;
import band.gosrock.api.event.model.dto.response.EventProfileResponse;
import band.gosrock.api.event.model.mapper.EventMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadUserEventProfilesUseCase {
    private final UserUtils userUtils;
    private final EventMapper eventMapper;

    public SliceResponse<EventProfileResponse> execute(SliceParam sliceParam) {
        final User user = userUtils.getCurrentUser();
        final Long userId = user.getId();
        return SliceResponse.of(eventMapper.toEventProfileResponseSlice(userId, sliceParam));
    }
}
