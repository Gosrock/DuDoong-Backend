package band.gosrock.domain.domains.cart.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.cart.exception.CartNotFoundException;
import band.gosrock.domain.domains.cart.repository.CartRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class CartAdaptor {
    private final CartRepository cartRepository;

    public Cart save(Cart cart) {
        return cartRepository.save(cart);
    }

    public Cart queryCart(Long cartId) {
        return cartRepository.findById(cartId).orElseThrow(() -> CartNotFoundException.EXCEPTION);
    }

    public Cart queryCart(Long cartId, Long userId) {
        return cartRepository
                .findByIdAndUserId(cartId, userId)
                .orElseThrow(() -> CartNotFoundException.EXCEPTION);
    }

    public Optional<Cart> findCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId);
    }

    public Cart find(Long cartId) {
        return cartRepository.find(cartId).orElseThrow(() -> CartNotFoundException.EXCEPTION);
    }

    //    public Cart upsert(Cart cart) {
    //        Objects.requireNonNull(cart.getUserId());
    //        return cartRepository
    //                .findByUserId(cart.getUserId())
    //                .orElseGet(() -> cartRepository.save(cart));
    //    }

    public void deleteByUserId(Long userId) {
        cartRepository.deleteByUserId(userId);
    }
}
