package band.gosrock.api.email.handler

import band.gosrock.api.email.service.EntranceIssuedTicketEmailService
import band.gosrock.api.email.service.IssuedTicketMailInfoHelper
import band.gosrock.domain.common.events.issuedTicket.EntranceIssuedTicketEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = LoggerFactory.getLogger(EntranceIssuedTicketEventEmailHandler::class.java)

@Component
class EntranceIssuedTicketEventEmailHandler(
    private val issuedTicketMailInfoHelper: IssuedTicketMailInfoHelper,
    private val entranceIssuedTicketEmailService: EntranceIssuedTicketEmailService,
) {
    @Async
    @TransactionalEventListener(
        classes = [EntranceIssuedTicketEvent::class],
        phase = TransactionPhase.AFTER_COMMIT,
    )
    fun handleEntranceIssuedTicketEvent(entranceIssuedTicketEvent: EntranceIssuedTicketEvent) {
        log.info("${entranceIssuedTicketEvent.issuedTicketNo}번 티켓 입장 처리 이메일 전송")

        val issuedTicketMailDTO =
            issuedTicketMailInfoHelper.execute(entranceIssuedTicketEvent.issuedTicketNo)
        entranceIssuedTicketEmailService.execute(issuedTicketMailDTO)
        log.info("이메일 전송 성공")
    }
}
