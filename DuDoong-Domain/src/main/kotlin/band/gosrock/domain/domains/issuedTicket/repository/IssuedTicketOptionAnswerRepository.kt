package band.gosrock.domain.domains.issuedTicket.repository

import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer
import org.springframework.data.jpa.repository.JpaRepository

interface IssuedTicketOptionAnswerRepository : JpaRepository<IssuedTicketOptionAnswer, Long>
