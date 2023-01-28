package band.gosrock.api.host.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.host.model.dto.response.HostProfileResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadHostListUseCase {
    private final UserUtils userUtils;
    private final HostAdaptor hostAdaptor;

    public List<HostProfileResponse> execute() {
        final User user = userUtils.getCurrentUser();
        final Long userId = user.getId();

        return hostAdaptor.findAllByHostUsers_UserId(userId).stream()
                .map(host -> HostProfileResponse.of(host, userId))
                .toList();
    }
}
