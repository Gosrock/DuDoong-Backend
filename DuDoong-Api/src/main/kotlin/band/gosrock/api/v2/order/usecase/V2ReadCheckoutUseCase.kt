package band.gosrock.api.v2.order.usecase

import band.gosrock.api.v2.event.usecase.V2ReadOnSaleTicketItemsUseCase
import band.gosrock.api.v2.order.dto.response.V2CheckoutResponse
import band.gosrock.api.v2.order.dto.response.V2MyOrderAccountResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.exception.TicketItemNotFoundException
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadCheckoutUseCase(
    private val readOnSaleTicketItemsUseCase: V2ReadOnSaleTicketItemsUseCase,
    private val ticketItemAdaptor: TicketItemAdaptor,
) {
    /**
     * O-0 결제 화면 (#726, 로그인 필요). [결제하기]·토스 송금 전에 입금 계좌·예금주를 보여 주기 위한 조회.
     * 티켓은 P-5 와 같은 판정(공개 공연 + 판매 중인 유효 티켓, 아니면 404)이고, 계좌는 두둥티켓(계좌 이체)만 준다. 공개 P-5 에는 계좌를 넣지 않는다
     */
    @Transactional(readOnly = true)
    fun execute(eventId: Long, ticketItemId: Long): V2CheckoutResponse {
        val ticket = readOnSaleTicketItemsUseCase.execute(eventId).firstOrNull { it.ticketItemId == ticketItemId }
            ?: throw TicketItemNotFoundException.EXCEPTION
        val item = ticketItemAdaptor.queryTicketItem(ticketItemId)
        return V2CheckoutResponse(
            ticket = ticket,
            account = item.accountInfo?.takeIf { item.payType == TicketPayType.DUDOONG_TICKET }?.let {
                V2MyOrderAccountResponse(bankName = it.bankName, accountHolder = it.accountHolder, accountNumber = it.accountNumber)
            },
        )
    }
}
