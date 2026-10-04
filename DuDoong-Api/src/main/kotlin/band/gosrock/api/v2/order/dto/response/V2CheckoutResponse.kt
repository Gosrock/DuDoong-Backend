package band.gosrock.api.v2.order.dto.response

import band.gosrock.api.v2.event.dto.response.V2PublicTicketItemResponse
import io.swagger.v3.oas.annotations.media.Schema

/** O-0 결제 화면 (로그인). 공개 티켓 정보(P-5 와 같음) + 입금 계좌 */
data class V2CheckoutResponse(
    @field:Schema(description = "티켓 (P-5 와 같은 형태: 잔여·매진·구매 가능은 승인 대기 차감)")
    val ticket: V2PublicTicketItemResponse,
    @field:Schema(
        description = "입금 계좌 (계좌 이체 방식 = 두둥티켓 + ticket.isPurchasable 일 때만. 무료·지난 공연·정산중·종료·매진은 null). O-3 payment.account 와 같은 형태 — 토스 송금 딥링크는 프론트가 이 값과 결제 금액으로 만든다",
    )
    val account: V2MyOrderAccountResponse?,
)
