package band.gosrock.api.event.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.common.page.PageResponse;
import band.gosrock.api.event.model.dto.response.EventProfileResponse;
import band.gosrock.api.event.model.mapper.EventMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadUserEventProfilesUseCase {
    private final UserUtils userUtils;
    private final EventMapper eventMapper;

    public PageResponse<EventProfileResponse> execute(Pageable pageable) {
        final User user = userUtils.getCurrentUser();
        final Long userId = user.getId();
        return PageResponse.of(eventMapper.toEventProfileResponsePage(userId, pageable));
    }
}
