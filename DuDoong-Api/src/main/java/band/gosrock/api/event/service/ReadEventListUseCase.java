package band.gosrock.api.event.service;


import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadEventListUseCase {
    private final HostAdaptor hostAdaptor;
    private final EventAdaptor eventAdaptor;

    public List<EventResponse> execute(Long hostId) {
        final Host host = hostAdaptor.findById(hostId);
        return eventAdaptor.findAllByHostId(hostId).stream()
                .map(EventResponse::of)
                .collect(Collectors.toList());
    }
}
