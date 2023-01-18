package band.gosrock.domain.domains.cart.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.cart.policy.CartPolicy;
import band.gosrock.domain.domains.ticket_item.repository.OptionGroupRepository;
import band.gosrock.domain.domains.ticket_item.repository.OptionRepository;
import band.gosrock.domain.domains.ticket_item.repository.TicketItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartDomainService {

    private final CartAdaptor cartAdaptor;

    private final CartPolicy cartPolicy;

    private final OptionRepository optionRepository;
    private final OptionGroupRepository optionGroupRepository;
    private final TicketItemRepository ticketItemRepository;

    // 한유저당 한 카트를 소유할 수 있는 제약조건을 가짐
    @RedissonLock(LockName = "카트생성", paramClassType = Cart.class, identifier = "userId")
    public Long createCart(Cart cart, Long userId) {
        cart.validItemKindPolicy(() -> cartPolicy);
        cartAdaptor.deleteByUserId(userId);
        Cart savedCart = cartAdaptor.save(cart);
        savedCart.validCorrectAnswerToItems();
        return savedCart.getId();
    }
}
