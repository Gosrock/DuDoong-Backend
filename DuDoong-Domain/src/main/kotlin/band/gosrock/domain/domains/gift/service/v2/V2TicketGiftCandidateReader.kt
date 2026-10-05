package band.gosrock.domain.domains.gift.service.v2

import band.gosrock.domain.domains.gift.domain.TicketGiftStatus
import band.gosrock.domain.domains.gift.repository.TicketGiftRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** 연쇄 취소 후보 (선물 id → 티켓 id) */
data class V2GiftCandidate(val giftId: Long, val issuedTicketId: Long)

/**
 * 보낸 사람 탈퇴·정지, 공연 운영 삭제 연쇄의 후보 대기 선물을 **짧은 별도 트랜잭션의 잠금 읽기(FOR SHARE)**로 읽는다 (#719 재리뷰).
 *
 * 원 트랜잭션은 사용자·공연 행을 먼저 X 로 잡는다(연쇄가 flush 로 상태 UPDATE 를 실행). 선물 생성(G-1)은 그 행을 FOR SHARE 로 잡고 커밋까지 놓지 않으므로
 * 이 시점에 그 사용자·공연의 선물 생성은 모두 커밋됐고 새로 생길 수도 없다.
 * - 원 트랜잭션의 일반 읽기(스냅샷)로는 부족하다: 스냅샷이 잠금 대기 전에 만들어져, 기다리는 동안 커밋된 생성을 못 본다 (E2E 결정적 경합으로 재현).
 *   잠금 읽기는 최신 커밋 값을 읽는다. 원 트랜잭션에서 선물을 다시 가져올 때도 잠금 읽기(선물마다 SELECT ... FOR UPDATE 1번)로 한다
 * - 원 트랜잭션에서 선물 행을 잠그지 않는 이유: 수락·회수·반환은 티켓 행 → 선물 행 순서로 잠근다. 연쇄가 선물 행을 쥔 채 티켓 행을 기다리면 역순이라 교착한다.
 *   그래서 선물 행 공유 잠금은 이 짧은 트랜잭션에서만 잡고 바로 놓은 뒤, 원 트랜잭션은 티켓 행(PK 순) → 선물 행(잠금 읽기) 순서로 잠근다.
 *   이 트랜잭션은 다른 잠금을 쥐지 않으므로 수락 중인 선물 행을 기다리더라도 교착하지 않는다
 */
@Component
class V2TicketGiftCandidateReader(private val ticketGiftRepository: TicketGiftRepository) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun pendingBySender(senderUserId: Long): List<V2GiftCandidate> =
        ticketGiftRepository.findAllBySenderLocked(senderUserId, TicketGiftStatus.PENDING).map { V2GiftCandidate(it.id!!, it.issuedTicketId) }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun pendingByEvent(eventId: Long): List<V2GiftCandidate> =
        ticketGiftRepository.findAllByEventLocked(eventId, TicketGiftStatus.PENDING).map { V2GiftCandidate(it.id!!, it.issuedTicketId) }
}
