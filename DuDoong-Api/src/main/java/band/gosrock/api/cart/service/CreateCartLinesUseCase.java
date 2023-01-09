package band.gosrock.api.cart.service;


import band.gosrock.api.cart.model.dto.request.AddCartLineDto;
import band.gosrock.api.cart.model.dto.request.AddCartOptionAnswerDto;
import band.gosrock.api.cart.model.dto.request.AddCartRequest;
import band.gosrock.api.cart.model.dto.response.CartItemOptionAnswerResponse;
import band.gosrock.api.cart.model.dto.response.CartItemResponse;
import band.gosrock.api.cart.model.dto.response.CreateCartResponse;
import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.cart.domain.CartLineItem;
import band.gosrock.domain.domains.cart.domain.CartOptionAnswer;
import band.gosrock.domain.domains.cart.service.CartDomainService;
import band.gosrock.domain.domains.event.repository.EventRepository;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.repository.OptionGroupRepository;
import band.gosrock.domain.domains.ticket_item.repository.OptionRepository;
import band.gosrock.domain.domains.ticket_item.repository.TicketItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateCartLinesUseCase {

    private final CartDomainService cartDomainService;
    private final CartAdaptor cartAdaptor;

    // 테스트 용
    private final OptionGroupRepository optionGroupRepository;
    // 테스트 용
    private final OptionRepository optionRepository;

    private final EventRepository eventRepository;

    private final TicketItemRepository ticketItemRepository;

    @Transactional
    public CreateCartResponse execute(AddCartRequest addCartRequest) {

        Long currentUserId = SecurityUtils.getCurrentUserId();
        // 테스트용 데이타
        //        Event event = eventRepository.save(Event.builder().name("고스락 제 20회 공연").build());
        //        Option optionYes =
        // Option.builder().additionalPrice(Money.ZERO).answer("예").build();
        //        Option optionNO =
        // Option.builder().additionalPrice(Money.ZERO).answer("아니오").build();
        //        OptionGroup build = OptionGroup.builder()
        //            .name("신한은행(110- )으로 3000원 입금 바랍니다.")
        //            .description("계좌이체 확인후 티켓을 발급해드립니다 ( 승인 결제 )")
        //            .type(OptionGroupType.TRUE_FALSE)
        //            .event(event)
        //            .options(List.of(optionNO,optionYes))
        //            .build();
        //        optionGroupRepository.save(build);
        //        TicketItem ticketItem =
        //                TicketItem.builder()
        //                        .type(TicketType.FIRST_COME_FIRST_SERVED)
        //                        .name("일반 티켓 선착순")
        //                        .price(Money.wons(3000L))
        //                        .build();
        //        ticketItemRepository.save(ticketItem);
        //
        //        Long id = optionYes.getId();
        //        OptionGroup optionGroup = optionYes.getOptionGroup();
        //        optionGroup.getName();
        // 요청에서 옵션 아이디만 추출
        //        List<Long> optionIds =
        //                addCartRequest.getAddCartLineDtos().stream()
        //                        .flatMap(addCartLineDto -> addCartLineDto.getOptionIds().stream())
        //                        .distinct()
        //                        .toList();
        // 캐싱
        //        optionRepository.findAllById(optionIds);
        //        List<Long> itemId = addCartLineDtos.stream()
        //            .map(addCartLineDto -> addCartLineDto.getItemId()).toList();

        List<AddCartLineDto> addCartLineDtos = addCartRequest.getItems();

        List<CartLineItem> cartLineItems =
                addCartLineDtos.stream()
                        .map(
                                addCartLineDto -> {
                                    List<CartOptionAnswer> answers =
                                            getCartOptionAnswers(addCartLineDto);
                                    // 기획상 itemId는 하나긴함
                                    // 겟 로직 티켓쪽에서 notfound 만들어준다 가정
                                    TicketItem ticketItem =
                                            ticketItemRepository
                                                    .findById(addCartLineDto.getItemId())
                                                    .get();
                                    return CartLineItem.builder()
                                            .ticketItem(ticketItem)
                                            .cartOptionAnswers(answers)
                                            .build();
                                })
                        .toList();

        Cart newCart =
                cartAdaptor.save(
                        Cart.builder().cartLineItems(cartLineItems).userId(currentUserId).build());
        List<CartItemResponse> cartItemResponses =
                newCart.getCartLineItems().stream()
                        .map(
                                cartLineItem -> {
                                    List<CartItemOptionAnswerResponse>
                                            cartItemOptionAnswerResponses =
                                                    getCartItemOptionAnswerResponses(cartLineItem);

                                    return CartItemResponse.builder()
                                            .answers(cartItemOptionAnswerResponses)
                                            .name(
                                                    cartLineItem.getTicketName()
                                                            + cartLineItem
                                                                    .getTicketPrice()
                                                                    .toString())
                                            .build();
                                })
                        .toList();
        return CreateCartResponse.builder()
                .items(cartItemResponses)
                .totalPrice(newCart.getTotalPrice().toString())
                .build();
    }

    @NotNull
    private static List<CartItemOptionAnswerResponse> getCartItemOptionAnswerResponses(
            CartLineItem cartLineItem) {
        return cartLineItem.getCartOptionAnswers().stream()
                .map(
                        cartOptionAnswer ->
                                CartItemOptionAnswerResponse.builder()
                                        .answer(cartOptionAnswer.getAnswer())
                                        .question(cartOptionAnswer.getQuestionName())
                                        .build())
                .toList();
    }

    private List<CartOptionAnswer> getCartOptionAnswers(AddCartLineDto addCartLineDto) {
        List<CartOptionAnswer> answers =
                addCartLineDto.getOptions().stream()
                        .map(this::getCartOptionAnswer)
                        .toList();
        return answers;
    }

    private CartOptionAnswer getCartOptionAnswer(AddCartOptionAnswerDto addCartOptionAnswerDto) {
        return CartOptionAnswer.builder()
                .option(optionRepository.findById(addCartOptionAnswerDto.getOptionId()).get())
                .answer(addCartOptionAnswerDto.getAnswer())
                .build();
    }
}
