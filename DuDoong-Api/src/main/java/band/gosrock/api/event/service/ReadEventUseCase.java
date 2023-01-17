package band.gosrock.api.event.service;

import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadEventUseCase {

    private final EventAdaptor eventAdaptor;

    public List<EventResponse> execute() {
        //Todo:: hostId로 변경필요
        return eventAdaptor.findAllByHostId(0L).stream()
                .map(EventResponse::of).collect(Collectors.toList());
    }
}
