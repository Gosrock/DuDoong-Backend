package band.gosrock.domain.domains.issuedTicket.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.LockNames
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

    @RedissonLock(LockName = LockNames.TICKET, identifier = "itemId")
    fun withdrawIssuedTicket(itemId: Long, issuedTickets: List<IssuedTicket>) {
        val ticketItem = ticketItemAdaptor.queryTicketItem(itemId)
        issuedTickets.forEach { issuedTicket ->
            ticketItem.increaseQuantity(1L)
            issuedTicket.cancel()
        }
    }

    @RedissonLock(LockName = LockNames.TICKET, identifier = "itemId")
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
        // uuid 조건의 잠금 읽기라 최신 커밋 값으로 찾는다 — 선물 수락·반환으로 uuid 가 바뀐 옛 QR 은 찾지 못해 404
        val issuedTicket = issuedTicketRepository.findByUuidForUpdate(uuid) ?: throw IssuedTicketNotFoundException.EXCEPTION
        issuedTicketValidator.validIssuedTicketEventIdEqualEvent(issuedTicket, eventId)
        ticketGiftGuard.validateNotGiftPendingLocked(issuedTicket)
        issuedTicket.entrance()
        return issuedTicket.toIssuedTicketInfoVo()
    }

    @RedissonLock(LockName = LockNames.TICKET, identifier = "itemId")
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
