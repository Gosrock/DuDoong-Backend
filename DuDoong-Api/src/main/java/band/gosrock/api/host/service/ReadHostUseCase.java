package band.gosrock.api.host.service;

import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.api.host.model.dto.response.HostResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadHostUseCase {
    private final UserDomainService userDomainService;
    private final HostAdaptor hostAdaptor;

    public List<HostResponse> execute() {
        Long securityUserId = SecurityUtils.getCurrentUserId();
        User user = userDomainService.retrieveUser(securityUserId);
        //Todo:: hostId로 변경필요
        return hostAdaptor.findAllByMasterUserId(securityUserId).stream()
                .map(HostResponse::of).collect(Collectors.toList());
    }
}
