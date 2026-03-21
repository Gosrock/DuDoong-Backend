package band.gosrock.domain.domains.issuedTicket.repository

import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface IssuedTicketRepository : JpaRepository<IssuedTicket, Long>, IssuedTicketCustomRepository {
    fun findAllByOrderLineId(orderLineId: Long): List<IssuedTicket>
    fun findAllByOrderUuid(orderId: String): List<IssuedTicket>
    fun findByIssuedTicketNo(issuedTicketNo: String): Optional<IssuedTicket>
    fun existsByEventId(eventId: Long): Boolean
    fun countByEventId(eventId: Long): Long
    fun findAllByEventId(eventId: Long, pageable: Pageable): Page<IssuedTicket>
    fun findAllByEventId(eventId: Long): List<IssuedTicket>
    fun findByUuid(uuid: String): Optional<IssuedTicket>
}
