package band.gosrock.api.v2.order.usecase

import band.gosrock.api.v2.event.usecase.V2PublicTicketItemMapper
import band.gosrock.api.v2.order.dto.response.V2CheckoutResponse
import band.gosrock.api.v2.order.dto.response.V2MyOrderAccountResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.exception.TicketItemNotFoundException
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadCheckoutUseCase(
    private val publicTicketItemMapper: V2PublicTicketItemMapper,
) {
    /**
     * O-0 결제 화면 (#726, 로그인 필요). [결제하기]·토스 송금 전에 입금 계좌·예금주를 보여 주기 위한 조회.
     * 티켓은 P-5 와 같은 판정(공개 공연 + 판매 중인 유효 티켓, 아니면 404). 계좌는 두둥티켓(계좌 이체)이고 **지금 살 수 있을 때만** 준다
     * (지난 공연·정산중·종료·매진이면 null — 입금하면 안 되는 상황). 공개 P-5 에는 계좌를 넣지 않는다
     */
    @Transactional(readOnly = true)
    fun execute(eventId: Long, ticketItemId: Long): V2CheckoutResponse {
        val (item, ticket) = publicTicketItemMapper.onSaleItems(eventId).firstOrNull { it.item.id == ticketItemId }
            ?: throw TicketItemNotFoundException.EXCEPTION
        return V2CheckoutResponse(
            ticket = ticket,
            account = item.accountInfo?.takeIf { item.payType == TicketPayType.DUDOONG_TICKET && ticket.isPurchasable }?.let {
                V2MyOrderAccountResponse(bankName = it.bankName, accountHolder = it.accountHolder, accountNumber = it.accountNumber)
            },
        )
    }
}
