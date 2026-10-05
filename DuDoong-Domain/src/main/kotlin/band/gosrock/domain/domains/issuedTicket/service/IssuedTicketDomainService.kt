package band.gosrock.domain.domains.issuedTicket.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.common.vo.IssuedTicketInfoVo
import band.gosrock.domain.domains.gift.service.TicketGiftGuard
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketNotFoundException
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import band.gosrock.domain.domains.issuedTicket.validator.IssuedTicketValidator
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@DomainService
@Transactional(readOnly = true)
class IssuedTicketDomainService(
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val userAdaptor: UserAdaptor,
    private val orderAdaptor: OrderAdaptor,
    private val issuedTicketValidator: IssuedTicketValidator,
    private val issuedTicketRepository: IssuedTicketRepository,
    private val ticketGiftGuard: TicketGiftGuard,
) {

    @RedissonLock(LockName = "티켓관리", identifier = "itemId")
    fun withdrawIssuedTicket(itemId: Long, issuedTickets: List<IssuedTicket>) {
        val ticketItem = ticketItemAdaptor.queryTicketItem(itemId)
        issuedTickets.forEach { issuedTicket ->
            ticketItem.increaseQuantity(1L)
            issuedTicket.cancel()
        }
    }

    @RedissonLock(LockName = "티켓관리", identifier = "itemId")
    fun doneOrderEventAfterRollBackWithdrawIssuedTickets(itemId: Long, orderUuid: String) {
        val failIssuedTickets = issuedTicketAdaptor.findAllByOrderUuid(orderUuid)
        val ticketItem = ticketItemAdaptor.queryTicketItem(itemId)
        failIssuedTickets.forEach { issuedTicket ->
            ticketItem.increaseQuantity(1L)
            issuedTicket.cancel()
        }
    }

    /**
     * v1 입장 처리. 티켓 행을 잠그고(#719 — 선물 생성·수락과 같은 행 잠금) 선물 대기 중이면 IssuedTicket_400_8.
     * 선물로 uuid 가 바뀐 옛 QR 은 없는 티켓(IssuedTicket_404_1)
     */
    fun processingEntranceIssuedTicket(eventId: Long, uuid: String): IssuedTicketInfoVo {
        // 잠금 읽기로 찾으므로 선물 수락·반환으로 uuid 가 바뀐 옛 QR 은 찾지 못한다. 영속성 컨텍스트에 먼저 올라간 옛 값이면 거부 (방어)
        val issuedTicket = issuedTicketRepository.findByUuidForUpdate(uuid)?.takeIf { it.uuid == uuid } ?: throw IssuedTicketNotFoundException.EXCEPTION
        issuedTicketValidator.validIssuedTicketEventIdEqualEvent(issuedTicket, eventId)
        ticketGiftGuard.validateNotGiftPendingLocked(issuedTicket)
        issuedTicket.entrance()
        return issuedTicket.toIssuedTicketInfoVo()
    }

    @RedissonLock(LockName = "티켓관리", identifier = "itemId")
    fun createIssuedTicket(itemId: Long, orderUuid: String, userId: Long) {
        val ticketItem = ticketItemAdaptor.queryTicketItem(itemId)
        val user = userAdaptor.queryUser(userId)
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        val orderLineItems = order.orderLineItems
        val issuedTickets = orderLineItems.flatMap { orderLineItem ->
            ticketItem.reduceQuantity(orderLineItem.quantity)
            IssuedTicket.orderLineToIssuedTicket(ticketItem, user, order, order.eventId, orderLineItem)
        }
        issuedTicketAdaptor.saveAll(issuedTickets)
    }
}
