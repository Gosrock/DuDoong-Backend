package band.gosrock.api.event.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.event.model.mapper.EventMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.service.EventService;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateEventDetailUseCase {
    private final UserUtils userUtils;
    private final HostAdaptor hostAdaptor;
    private final EventService eventService;
    private final EventMapper eventMapper;
}
