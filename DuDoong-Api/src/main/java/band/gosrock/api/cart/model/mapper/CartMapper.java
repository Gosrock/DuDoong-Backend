package band.gosrock.api.cart.model.mapper;


import band.gosrock.api.cart.model.dto.request.AddCartLineDto;
import band.gosrock.api.cart.model.dto.request.AddCartOptionAnswerDto;
import band.gosrock.api.cart.model.dto.request.AddCartRequest;
import band.gosrock.api.cart.model.dto.response.CartItemResponse;
import band.gosrock.api.cart.model.dto.response.CartResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.cart.domain.CartLineItem;
import band.gosrock.domain.domains.cart.domain.CartOptionAnswer;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.repository.OptionRepository;
import band.gosrock.domain.domains.ticket_item.repository.TicketItemRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class CartMapper {
    private final TicketItemRepository ticketItemRepository;
    private final OptionRepository optionRepository;

    private final CartAdaptor cartAdaptor;

    @Transactional(readOnly = true)
    public CartResponse toCartResponse(Long cartId) {
        Cart cart = cartAdaptor.queryCart(cartId);

        List<CartLineItem> newCartLineItems = cart.getCartLineItems();
        Long totalQuantity = cart.getTotalQuantity();

        int startNum = 1;
        List<CartItemResponse> cartItemResponses = new ArrayList<>();
        for (CartLineItem cartLineItem : newCartLineItems) {
            cartItemResponses.add(
                    CartItemResponse.of(
                            generateCartLineName(cartLineItem, startNum, totalQuantity),
                            cartLineItem));
            startNum += cartLineItem.getQuantity();
        }

        return CartResponse.of(cartItemResponses, cart);
    }

    public Cart toEntity(AddCartRequest addCartRequest, Long currentUserId) {
        List<AddCartLineDto> addCartLineDtos = addCartRequest.getItems();

        List<CartLineItem> cartLineItems =
                addCartLineDtos.stream()
                        .map(
                                addCartLineDto ->
                                        CartLineItem.builder()
                                                .ticketItem(getTicketItem(addCartLineDto))
                                                .cartOptionAnswers(
                                                        getCartOptionAnswers(addCartLineDto))
                                                .quantity(addCartLineDto.getQuantity())
                                                .build())
                        .toList();
        return Cart.builder().cartLineItems(cartLineItems).userId(currentUserId).build();
    }

    @NotNull
    private TicketItem getTicketItem(AddCartLineDto addCartLineDto) {
        return ticketItemRepository.findById(addCartLineDto.getItemId()).get();
    }

    /**
     * 일반티켓(1/3) - 4000원 과 같은 장바구니 상품의 응답을 반환합니다 카트라인 기준입니다. 카트라인에 3개의 상품이 같이 담기면 ( 옵션이 같은 상황 )
     * 일반티켓(1-3/3) 이런식으로 표현됩니다.
     *
     * @param cartLineItem
     * @param startNum
     * @param totalQuantity
     * @return
     */
    private String generateCartLineName(
            CartLineItem cartLineItem, int startNum, Long totalQuantity) {
        Long cartLineQuantity = cartLineItem.getQuantity();
        if (cartLineQuantity.equals(1L)) {
            return String.format(
                    "%s (%s/%d) - %s",
                    cartLineItem.getTicketName(),
                    startNum,
                    totalQuantity,
                    cartLineItem.getTotalCartLinePrice().toString());
        }
        int endNum = (int) (cartLineQuantity - 1 + startNum);
        return String.format(
                "%s (%s/%d) - %s",
                cartLineItem.getTicketName(),
                startNum + "-" + endNum,
                totalQuantity,
                cartLineItem.getTotalCartLinePrice().toString());
    }

    private List<CartOptionAnswer> getCartOptionAnswers(AddCartLineDto addCartLineDto) {
        return addCartLineDto.getOptions().stream().map(this::getCartOptionAnswer).toList();
    }

    private CartOptionAnswer getCartOptionAnswer(AddCartOptionAnswerDto addCartOptionAnswerDto) {
        return CartOptionAnswer.builder()
                .option(optionRepository.findById(addCartOptionAnswerDto.getOptionId()).get())
                .answer(addCartOptionAnswerDto.getAnswer())
                .build();
    }
}
