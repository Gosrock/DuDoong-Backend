package band.gosrock.api.email.handler;


import band.gosrock.api.email.dto.IssuedTicketMailDTO;
import band.gosrock.api.email.service.EntranceIssuedTicketEmailService;
import band.gosrock.api.email.service.IssuedTicketMailInfoHelper;
import band.gosrock.domain.common.events.issuedTicket.EntranceIssuedTicketEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntranceIssuedTicketEventEmailHandler {

    private final IssuedTicketMailInfoHelper issuedTicketMailInfoHelper;

    private final EntranceIssuedTicketEmailService entranceIssuedTicketEmailService;

    @Async
    @TransactionalEventListener(
            classes = EntranceIssuedTicketEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleEntranceIssuedTicketEvent(
            EntranceIssuedTicketEvent entranceIssuedTicketEvent) {
        log.info(entranceIssuedTicketEvent.getIssuedTicketNo() + "번 티켓 입장 처리 이메일 전송");

        IssuedTicketMailDTO issuedTicketMailDTO =
                issuedTicketMailInfoHelper.execute(entranceIssuedTicketEvent.getIssuedTicketNo());
        entranceIssuedTicketEmailService.execute(issuedTicketMailDTO);
        log.info("이메일 전송 성공");
    }
}
