package band.gosrock.domain.domains.gift.service.v2

import band.gosrock.domain.domains.gift.domain.TicketGiftStatus
import band.gosrock.domain.domains.gift.repository.TicketGiftRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** 연쇄 취소 후보 (선물 id → 티켓 id) */
data class V2GiftCandidate(val giftId: Long, val issuedTicketId: Long)

/**
 * 보낸 사람 탈퇴·정지, 공연 운영 삭제 연쇄의 후보 대기 선물을 **새 트랜잭션(새 스냅샷)**으로 읽는다 (#719 재리뷰).
 *
 * 원 트랜잭션은 사용자·공연 행을 이미 X 로 잠갔다. 선물 생성(G-1)은 그 행을 FOR SHARE 로 잡고 커밋까지 놓지 않으므로,
 * 이 시점에는 그 사용자·공연의 선물 생성이 모두 커밋됐고 새로 생길 수도 없다. 다만 원 트랜잭션의 REPEATABLE READ 스냅샷은
 * 잠금을 기다리기 전에 만들어졌을 수 있어 그 사이 커밋된 선물을 못 본다 → 새 스냅샷으로 읽는다.
 *
 * 선물 행을 잠금 읽기로 먼저 잡지 않는 이유: 수락·회수·반환은 티켓 행 → 선물 행 순서로 잠근다. 연쇄가 선물 행을 먼저 잡고 티켓 행을 기다리면
 * 그 순서와 반대라 교착이 생긴다. 그래서 후보는 새 스냅샷으로 찾고, 티켓 행(PK 순) → 선물 행(잠금 읽기) 순서로 잠근 뒤 판정한다
 */
@Component
class V2TicketGiftCandidateReader(private val ticketGiftRepository: TicketGiftRepository) {

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun pendingBySender(senderUserId: Long): List<V2GiftCandidate> =
        ticketGiftRepository.findAllBySenderUserIdAndStatus(senderUserId, TicketGiftStatus.PENDING).map { V2GiftCandidate(it.id!!, it.issuedTicketId) }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun pendingByEvent(eventId: Long): List<V2GiftCandidate> =
        ticketGiftRepository.findAllByEventIdAndStatus(eventId, TicketGiftStatus.PENDING).map { V2GiftCandidate(it.id!!, it.issuedTicketId) }
}
