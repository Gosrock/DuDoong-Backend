package band.gosrock.api.order.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.api.order.model.mapper.OrderMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.order.service.OrderApproveService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ApproveOrderUseCase {

    private final OrderApproveService orderApproveService;

    private final OrderMapper orderMapper;
    private final HostAdaptor hostAdaptor;
    private final EventAdaptor eventAdaptor;
    private final UserUtils userUtils;

    public OrderResponse execute(Long eventId, String orderUuid) {
        Event event = eventAdaptor.findById(eventId);
        Host host = hostAdaptor.findById(event.getHostId());
        Long userId = userUtils.getCurrentUserId();
        host.validateHostUser(userId);

        String confirmOrderUuid = orderApproveService.execute(orderUuid);
        return orderMapper.toOrderResponse(confirmOrderUuid);
    }
}
