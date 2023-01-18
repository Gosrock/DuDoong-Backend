package band.gosrock.domain.domains.cart.repository;

import band.gosrock.domain.domains.cart.domain.Cart;
import java.util.Optional;

public interface CartCustomRepository {
    public Optional<Cart> find(Long cartId);
}
