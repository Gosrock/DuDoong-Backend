package band.gosrock.api.host.service;


import band.gosrock.api.common.page.PageResponse;
import band.gosrock.api.host.model.dto.response.HostEventProfileResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadHostEventsUseCase {
    private final HostAdaptor hostAdaptor;
    private final EventAdaptor eventAdaptor;

    public PageResponse<HostEventProfileResponse> execute(Long hostId, Pageable pageable) {
        Host host = hostAdaptor.findById(hostId);
        return PageResponse.of(
                eventAdaptor
                        .findAllByHostId(hostId, pageable)
                        .map(event -> HostEventProfileResponse.of(host, event)));
    }
}
