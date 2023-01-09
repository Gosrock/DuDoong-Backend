package band.gosrock.api.cart.service;


import band.gosrock.api.cart.model.dto.request.AddCartRequest;
import band.gosrock.api.cart.model.dto.response.CreateCartResponse;
import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.cart.service.CartDomainService;
import band.gosrock.domain.domains.ticket_item.domain.Option;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup;
import band.gosrock.domain.domains.ticket_item.repository.OptionGroupRepository;
import band.gosrock.domain.domains.ticket_item.repository.OptionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public CreateCartResponse execute(AddCartRequest addCartRequest) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Option option = Option.builder().additionalPrice(Money.wons(1000L)).answer("예").build();
        OptionGroup optionGroup =
                optionGroupRepository.save(
                        OptionGroup.builder()
                                .name("뒷풀이 참석 여부 조사")
                                .description("오케이포차 조사입니다.")
                                .options(List.of(option))
                                .build());
        //        List<List<AddCartOptionAnswerDto>> lists =
        // addCartRequest.getAddCartLineDtos().stream()
        //            .map(A).toList();
        //        lists.stream().map(addCartOptionAnswerDtos -> addCartOptionAnswerDtos)
        //        List<CartLineItem> cartLines = addCartRequest.getCartLines(currentUserId);

        Cart save =
                cartAdaptor.save(
                        Cart.builder().cartLineItems(cartLines).userId(currentUserId).build());

        //        CartItemOptionAnswerResponse cartItemOptionAnswerResponse =
        // CartItemOptionAnswerResponse.builder().answer()
        //            .question().build();
        //        CartItemResponse cartItemResponse =
        // CartItemResponse.builder().answers().title().build();
        //        CreateCartResponse createCartResponse =
        // CreateCartResponse.builder().cartItemDtos().amount().build();
        return null;
    }
}
