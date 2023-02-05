package band.gosrock.api.email.service;

import band.gosrock.api.email.dto.OrderMailDto;
import band.gosrock.common.annotation.Helper;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.infrastructure.config.mail.dto.EmailEventInfo;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

@Helper
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderMailInfoHelper {
    private final OrderAdaptor orderAdaptor;
    private final UserAdaptor userAdaptor;
    private final EventAdaptor eventAdaptor;
    private final HostAdaptor hostAdaptor;

    public OrderMailDto execute(String orderUuid){
        Order order = orderAdaptor.find(orderUuid);
        User user = userAdaptor.queryUser(order.getUserId());
        Event event = eventAdaptor.findById(order.getEventId());
        Host host= hostAdaptor.findById(event.getHostId());
        return new OrderMailDto(user.toEmailUserInfo(),order.toEmailOrderInfo(),getEventInfo(event,
            host));
    }

    private EmailEventInfo getEventInfo(Event event, Host host) {
        return new EmailEventInfo(host.getProfile().getName(),
            event.getEventBasic().getName());
    }
}
