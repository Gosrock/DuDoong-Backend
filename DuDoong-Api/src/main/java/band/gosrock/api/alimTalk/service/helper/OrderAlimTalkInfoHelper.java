package band.gosrock.api.alimTalk.service.helper;


import band.gosrock.api.alimTalk.dto.OrderAlimTalkDto;
import band.gosrock.common.annotation.Helper;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.domain.Host;
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
    private final UserAdaptor userAdaptor;

    public OrderAlimTalkDto execute(Order order, Event event, Host host)
            throws NumberParseException {
        User user = userAdaptor.queryUser(order.getUserId());
        return new OrderAlimTalkDto(
                user.toAlimTalkUserInfo(), order.toAlimTalkOrderInfo(), getEventInfo(event, host));
    }

    private AlimTalkEventInfo getEventInfo(Event event, Host host) {
        return new AlimTalkEventInfo(host.getProfile().getName(), event.getEventBasic().getName());
    }
}
