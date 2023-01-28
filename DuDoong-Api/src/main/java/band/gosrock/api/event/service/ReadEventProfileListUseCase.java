package band.gosrock.api.event.service;


import band.gosrock.api.common.page.PageResponse;
import band.gosrock.api.event.model.dto.response.EventProfileResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadEventProfileListUseCase {
    private final EventAdaptor eventAdaptor;

    public PageResponse<EventProfileResponse> execute(Long hostId, Pageable pageable) {
        return PageResponse.of(
                eventAdaptor.findAllByHostId(hostId, pageable).map(EventProfileResponse::of));
    }
}
