package band.gosrock.api.host.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.host.model.dto.response.HostResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadHostListUseCase {
    private final UserDomainService userDomainService;
    private final HostAdaptor hostAdaptor;

    public List<HostResponse> execute() {
        Long securityUserId = SecurityUtils.getCurrentUserId();
        User user = userDomainService.retrieveUser(securityUserId);
        return hostAdaptor.findAllByHostUsers_UserId(securityUserId).stream()
                .map(HostResponse::of)
                .toList();
    }
}
