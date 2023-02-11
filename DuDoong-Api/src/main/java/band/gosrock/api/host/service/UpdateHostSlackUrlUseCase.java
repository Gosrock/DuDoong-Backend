package band.gosrock.api.host.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.host.model.dto.request.UpdateHostSlackRequest;
import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.api.host.model.mapper.HostMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.exception.InvalidSlackUrlException;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.infrastructure.config.slack.SlackMessageProvider;
import java.net.UnknownHostException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateHostSlackUrlUseCase {
    private final HostService hostService;
    private final HostAdaptor hostAdaptor;
    private final HostMapper hostMapper;

    private final SlackMessageProvider slackMessageProvider;

    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    public HostDetailResponse execute(Long hostId, UpdateHostSlackRequest updateHostSlackRequest) {
        final Host host = hostAdaptor.findById(hostId);
        final String slackUrl = updateHostSlackRequest.getSlackUrl();
        hostService.validateDuplicatedSlackUrl(host, slackUrl);

        try {
            slackMessageProvider.register(slackUrl);
            return hostMapper.toHostDetailResponse(hostService.updateHostSlackUrl(host, slackUrl));
        } catch (UnknownHostException e) {
            throw InvalidSlackUrlException.EXCEPTION;
        }
    }
}
