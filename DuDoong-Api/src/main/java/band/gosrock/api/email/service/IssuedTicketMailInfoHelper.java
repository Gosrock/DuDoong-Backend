package band.gosrock.api.email.service;


import band.gosrock.api.email.dto.IssuedTicketMailDTO;
import band.gosrock.common.annotation.Helper;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.infrastructure.config.mail.dto.EmailEventInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Helper
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssuedTicketMailInfoHelper {

    private final UserAdaptor userAdaptor;

    private final EventAdaptor eventAdaptor;

    private final IssuedTicketAdaptor issuedTicketAdaptor;

    private final HostAdaptor hostAdaptor;

    public IssuedTicketMailDTO execute(String issuedTicketNo) {
        IssuedTicket issuedTicket = issuedTicketAdaptor.queryByIssuedTicketNo(issuedTicketNo);
        User user = userAdaptor.queryUser(issuedTicket.getUserInfo().getUserId());
        Event event = eventAdaptor.findById(issuedTicket.getEventId());
        Host host = hostAdaptor.findById(event.getHostId());
        return new IssuedTicketMailDTO(
                user.toEmailUserInfo(),
                issuedTicket.toEmailIssuedTicketInfo(),
                getEventInfo(event, host));
    }

    private EmailEventInfo getEventInfo(Event event, Host host) {
        return new EmailEventInfo(host.getProfile().getName(), event.getEventBasic().getName());
    }
}
