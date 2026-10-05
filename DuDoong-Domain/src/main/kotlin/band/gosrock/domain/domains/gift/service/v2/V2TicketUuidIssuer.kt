package band.gosrock.domain.domains.gift.service.v2

import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * 선물 수락·반환 때 쓸 새 티켓 uuid (#719, DEC-026 #6). 이미 있는 값이면 다시 뽑는다 (uk_issued_ticket_uuid 충돌 방지).
 * [candidate] 는 테스트에서 충돌을 재현하려고 열어 둔다
 */
@Component
class V2TicketUuidIssuer(private val issuedTicketRepository: IssuedTicketRepository) {

    fun issue(): String {
        repeat(MAX_ATTEMPTS) {
            val uuid = candidate()
            if (!issuedTicketRepository.existsByUuid(uuid)) return uuid
        }
        throw IllegalStateException("티켓 uuid 를 ${MAX_ATTEMPTS}번 연속 중복으로 뽑음")
    }

    fun candidate(): String = UUID.randomUUID().toString()

    companion object {
        const val MAX_ATTEMPTS = 5
    }
}
