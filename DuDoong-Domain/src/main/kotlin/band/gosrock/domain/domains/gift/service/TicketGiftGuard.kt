package band.gosrock.domain.domains.gift.service

import band.gosrock.common.annotation.Validator
import band.gosrock.domain.domains.gift.domain.TicketGiftStatus
import band.gosrock.domain.domains.gift.repository.TicketGiftRepository
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketGiftPendingException
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.exception.V2OrderCannotCancelByUserException

/**
 * 선물이 생겨서 필요한 v1·v2 공통 보호 (#719, 11 문서 8-2 경로별 차단 표·8-3).
 * 선물 데이터(`tbl_ticket_gift`)는 v2 전용이지만 소유자·입장 가능 여부는 v1 경로도 지켜야 하므로, v1 코드가 부를 수 있도록
 * `service.v2` 밖에 둔다 (읽기만. 선물 전이는 `service.v2.V2TicketGiftDomainService`).
 * 선물을 쓰지 않은 티켓·주문은 모든 판정이 기존과 같다.
 *
 * 대기 중 선물은 티켓당 최대 1개다 (생성이 `주문` 락 + 티켓 행 잠금 안에서 대기 선물이 없을 때만 된다)
 */
@Validator
class TicketGiftGuard(
    private val ticketGiftRepository: TicketGiftRepository,
    private val issuedTicketRepository: IssuedTicketRepository,
) {

    /** 화면용 (잠금 없음). 입장 판정에는 [isGiftPendingLocked] */
    fun isGiftPending(issuedTicketId: Long): Boolean =
        ticketGiftRepository.existsByIssuedTicketIdAndStatus(issuedTicketId, TicketGiftStatus.PENDING)

    /**
     * 입장 판정용: 티켓 행을 `FOR UPDATE` 로 잠근 **뒤에** 부른다. 선물 행을 공유 잠금 읽기로 읽어 최신 커밋 값을 본다
     * (일반 읽기는 REPEATABLE READ 스냅샷이라 티켓 잠금을 기다리는 동안 커밋된 선물 생성을 놓친다, #719 리뷰)
     */
    fun isGiftPendingLocked(issuedTicketId: Long): Boolean =
        ticketGiftRepository.findAllByTicketAndStatusLocked(issuedTicketId, TicketGiftStatus.PENDING).isNotEmpty()

    /** v1 티켓 상세(화면): 선물 대기 중이면 IssuedTicket_400_8 */
    fun validateNotGiftPending(issuedTicket: IssuedTicket) {
        if (isGiftPending(issuedTicket.id!!)) throw IssuedTicketGiftPendingException.EXCEPTION
    }

    /** v1 입장: 티켓 행을 잠근 뒤 잠금 읽기로 선물 대기를 확인해 IssuedTicket_400_8 */
    fun validateNotGiftPendingLocked(issuedTicket: IssuedTicket) {
        if (isGiftPendingLocked(issuedTicket.id!!)) throw IssuedTicketGiftPendingException.EXCEPTION
    }

    /** 대기 중 선물이 걸린 티켓 id */
    fun pendingTicketIds(issuedTicketIds: Collection<Long>): Set<Long> =
        if (issuedTicketIds.isEmpty()) emptySet()
        else ticketGiftRepository.findAllByIssuedTicketIdInAndStatus(issuedTicketIds, TicketGiftStatus.PENDING).map { it.issuedTicketId }.toSet()

    /**
     * 사용자 취소를 막는 선물 티켓이 있는지 (v2 O-4·v1 사용자 환불 공통, 기본안 15): 취소되지 않은 티켓 중 주문자 소유가 아닌 것(선물 완료) 또는 선물 대기.
     * `주문` 락 안에서 부른다 (선물 생성도 같은 락이라 판정과 생성이 겹치지 않는다)
     */
    fun hasUserCancelBlockingGift(order: Order): Boolean = order.uuid!! in userCancelBlockedOrderUuids(listOf(order))

    /**
     * [hasUserCancelBlockingGift] 의 묶음 판정 (v1 주문 목록, #734 N+1 제거): 주문 수와 무관하게 발급 티켓 조회 1번 + 선물 대기 조회 1번.
     * @return 사용자 취소를 막는 선물 티켓이 있는 주문 uuid
     */
    fun userCancelBlockedOrderUuids(orders: Collection<Order>): Set<String> {
        val owners = orders.associate { it.uuid!! to it.userId }
        if (owners.isEmpty()) return emptySet()
        val tickets = issuedTicketRepository.findAllByOrderUuidIn(owners.keys).filterNot { it.issuedTicketStatus.isCanceled() }
        val pending = pendingTicketIds(tickets.mapNotNull { it.id })
        return tickets.filter { it.getUserId() != owners[it.orderUuid] || it.id in pending }.mapNotNull { it.orderUuid }.toSet()
    }

    /** v1 사용자 환불: 선물 대기·선물 완료 티켓이 있으면 Order_400_24 (v2 O-4 와 같은 코드) */
    fun validateNoUserCancelBlockingGift(order: Order) {
        if (hasUserCancelBlockingGift(order)) throw V2OrderCannotCancelByUserException.EXCEPTION
    }
}
