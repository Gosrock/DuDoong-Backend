package band.gosrock.api.host.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.common.page.PageResponse;
import band.gosrock.api.host.model.dto.response.HostProfileResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadHostProfilesUseCase {
    private final UserUtils userUtils;
    private final HostAdaptor hostAdaptor;

    public PageResponse<HostProfileResponse> execute(Pageable pageable) {
        final User user = userUtils.getCurrentUser();
        final Long userId = user.getId();

        return PageResponse.of(
                hostAdaptor
                        .findAllByHostUsers_UserId(userId, pageable)
                        .map(host -> HostProfileResponse.of(host, userId)));
    }
}
