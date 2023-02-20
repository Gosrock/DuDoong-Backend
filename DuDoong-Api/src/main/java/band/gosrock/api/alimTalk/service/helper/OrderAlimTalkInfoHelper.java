package band.gosrock.api.alimTalk.service.helper;


import band.gosrock.api.alimTalk.dto.OrderAlimTalkDto;
import band.gosrock.common.annotation.Helper;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkEventInfo;
import com.google.i18n.phonenumbers.NumberParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Helper
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderAlimTalkInfoHelper {

    private final OrderAdaptor orderAdaptor;
    private final UserAdaptor userAdaptor;
    private final EventAdaptor eventAdaptor;
    private final HostAdaptor hostAdaptor;

    public OrderAlimTalkDto execute(String orderUuid) throws NumberParseException {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        User user = userAdaptor.queryUser(order.getUserId());
        Event event = eventAdaptor.findById(order.getEventId());
        Host host = hostAdaptor.findById(event.getHostId());
        return new OrderAlimTalkDto(user.toAlimTalkUserInfo(), getEventInfo(event, host));
    }

    private AlimTalkEventInfo getEventInfo(Event event, Host host) {
        return new AlimTalkEventInfo(host.getProfile().getName(), event.getEventBasic().getName());
    }
}
