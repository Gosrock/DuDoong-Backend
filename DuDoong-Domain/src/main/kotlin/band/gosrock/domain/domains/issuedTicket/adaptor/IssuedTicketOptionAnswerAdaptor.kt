package band.gosrock.domain.domains.issuedTicket.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketOptionAnswerRepository

@Adaptor
class IssuedTicketOptionAnswerAdaptor(
    private val issuedTicketOptionAnswerRepository: IssuedTicketOptionAnswerRepository,
) {
    fun saveAll(issuedTicketOptionAnswers: List<IssuedTicketOptionAnswer>) {
        issuedTicketOptionAnswerRepository.saveAll(issuedTicketOptionAnswers)
    }
}
