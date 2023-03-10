package band.gosrock.api.cart.model.dto.response;


import band.gosrock.domain.common.vo.AccountInfoVo;
import band.gosrock.domain.common.vo.EventProfileVo;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType;
import band.gosrock.domain.domains.ticket_item.domain.TicketType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartResponse {
    @Schema(description = "장바구니명 입니다.", defaultValue = "")
    private final String title;
    // 내티켓 확인하기
    private final List<CartItemResponse> items;

    // 금액
    @Schema(description = "카트라인들의 총 결제금액을 합한 금액입니다", defaultValue = "15000원")
    private final Money totalPrice;

    @Schema(description = "생성한 장바구니의 아이디입니다", defaultValue = "30")
    private final Long cartId;

    @Schema(description = "전체 아이템 수량을 의미합니다", defaultValue = "3")
    private final Long totalQuantity;

    @Schema(
            description = "결제가 필요한지에 대한 여부를 결정합니다. 필요한 true면 결제창 띄우시면됩니다. 이단계에선 무시하셔도 됩니다.",
            defaultValue = "true")
    private final Boolean isNeedPayment;

    @Schema(description = "티켓의 타입. 승인 , 선착순 두가지입니다.")
    private final TicketType approveType;

    @Schema(description = "티켓의 지불 타입. 두둥티켓, 무료 , 유료 세가지입니다.")
    private final TicketPayType ticketPayType;

    @Schema(description = "계좌정보", nullable = true)
    private final AccountInfoVo accountInfo;

    @Schema(description = "이벤트 정보")
    private final EventProfileVo eventProfile;

    public static CartResponse of(
            List<CartItemResponse> cartItemResponses, Cart cart, TicketItem item, Event event) {
        return CartResponse.builder()
                .items(cartItemResponses)
                .totalPrice(cart.getTotalPrice())
                .cartId(cart.getId())
                .title(cart.getCartName())
                .isNeedPayment(cart.isNeedPaid())
                .totalQuantity(cart.getTotalQuantity())
                .approveType(item.getType())
                .ticketPayType(item.getPayType())
                .accountInfo(item.getAccountInfo())
                .eventProfile(event.toEventProfileVo())
                .build();
    }
}
