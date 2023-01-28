package band.gosrock.api.cart.model.mapper;


import band.gosrock.api.cart.model.dto.request.AddCartLineDto;
import band.gosrock.api.cart.model.dto.request.AddCartOptionAnswerDto;
import band.gosrock.api.cart.model.dto.request.AddCartRequest;
import band.gosrock.api.cart.model.dto.response.CartItemResponse;
import band.gosrock.api.cart.model.dto.response.CartResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.cart.domain.CartLineItem;
import band.gosrock.domain.domains.cart.domain.CartOptionAnswer;
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.Option;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.repository.OptionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class CartMapper {
    private final TicketItemAdaptor ticketItemAdaptor;
    private final OptionAdaptor optionAdaptor;
    private final OptionRepository optionRepository;

    private final CartAdaptor cartAdaptor;

    @Transactional(readOnly = true)
    public CartResponse toCartResponse(Long cartId) {
        Cart cart = cartAdaptor.queryCart(cartId);
        return getCartResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse toCartResponse(Cart cart) {
        return getCartResponse(cart);
    }

    private CartResponse getCartResponse(Cart cart) {
        List<CartLineItem> newCartLineItems = cart.getCartLineItems();
        TicketItem ticketItem = ticketItemAdaptor.queryTicketItem(cart.getItemId());

        List<CartItemResponse> cartItemResponses =
                getCartItemResponses(newCartLineItems, ticketItem.getName());

        return CartResponse.of(cartItemResponses, cart, ticketItem);
    }

    private List<CartItemResponse> getCartItemResponses(
            List<CartLineItem> newCartLineItems, String name) {
        return newCartLineItems.stream()
                .map(cartLineItem -> getCartItemResponse(cartLineItem, name))
                .toList();
    }

    private CartItemResponse getCartItemResponse(CartLineItem cartLineItem, String itemName) {
        return CartItemResponse.of(
                cartLineItem,
                generateCartLineName(itemName, cartLineItem.getQuantity()),
                getOptionAnswerVos(cartLineItem));
    }

    private List<OptionAnswerVo> getOptionAnswerVos(CartLineItem cartLineItem) {
        List<CartOptionAnswer> cartOptionAnswers = cartLineItem.getCartOptionAnswers();
        List<Option> options = optionAdaptor.findAllByIds(getOptionIds(cartOptionAnswers));
        return cartOptionAnswers.stream()
                .map(
                        cartOptionAnswer ->
                                cartOptionAnswer.getOptionAnswerVo(
                                        findOption(options, cartOptionAnswer)))
                .toList();
    }

    private static List<Long> getOptionIds(List<CartOptionAnswer> cartOptionAnswers) {
        return cartOptionAnswers.stream().map(CartOptionAnswer::getOptionId).toList();
    }

    private static Option findOption(List<Option> options, CartOptionAnswer cartOptionAnswer) {
        return options.stream()
                .filter(option -> option.getId().equals(cartOptionAnswer.getOptionId()))
                .findFirst()
                .orElseThrow();
    }

    public Cart toEntity(AddCartRequest addCartRequest, Long currentUserId) {
        List<AddCartLineDto> addCartLineDtos = addCartRequest.getItems();
        List<CartLineItem> cartLineItems =
                addCartLineDtos.stream()
                        .map(
                                addCartLineDto ->
                                        CartLineItem.builder()
                                                .item(getTicketItem(addCartLineDto))
                                                .cartOptionAnswers(
                                                        getCartOptionAnswers(addCartLineDto))
                                                .quantity(addCartLineDto.getQuantity())
                                                .build())
                        .toList();
        return Cart.builder().cartLineItems(cartLineItems).userId(currentUserId).build();
    }

    private TicketItem getTicketItem(AddCartLineDto addCartLineDto) {
        return ticketItemAdaptor.queryTicketItem(addCartLineDto.getItemId());
    }

    private String generateCartLineName(String itemName, Long quantity) {
        return String.format("%s (%d)매", itemName, quantity);
    }

    private List<CartOptionAnswer> getCartOptionAnswers(AddCartLineDto addCartLineDto) {
        return addCartLineDto.getOptions().stream().map(this::getCartOptionAnswer).toList();
    }

    private CartOptionAnswer getCartOptionAnswer(AddCartOptionAnswerDto addCartOptionAnswerDto) {
        return CartOptionAnswer.builder()
                .option(optionAdaptor.queryOption(addCartOptionAnswerDto.getOptionId()))
                .answer(addCartOptionAnswerDto.getAnswer())
                .build();
    }
}
