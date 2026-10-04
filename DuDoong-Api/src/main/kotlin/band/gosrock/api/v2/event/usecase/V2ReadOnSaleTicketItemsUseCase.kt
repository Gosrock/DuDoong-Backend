package band.gosrock.api.v2.event.usecase

import band.gosrock.api.v2.event.dto.response.V2PublicTicketItemResponse
import band.gosrock.common.annotation.UseCase
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadOnSaleTicketItemsUseCase(
    private val publicTicketItemMapper: V2PublicTicketItemMapper,
) {
    /**
     * 공개 API. 판매 중인 유효 티켓만, 생성 순 (DEC-020, [V2PublicTicketItemMapper]).
     * 준비중·삭제 공연은 404. 지난 공연도 목록은 보이고 isPurchasable=false. 입금 계좌는 내려주지 않는다 (로그인 후 결제 화면 O-0 에서 제공)
     */
    @Transactional(readOnly = true)
    fun execute(eventId: Long): List<V2PublicTicketItemResponse> = publicTicketItemMapper.onSaleItems(eventId).map { it.response }
}
