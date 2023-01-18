package band.gosrock.domain.domains.cart.repository;


import band.gosrock.domain.domains.cart.domain.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> ,CartCustomRepository{
    Optional<Cart> findByIdAndUserId(Long id, Long userId);

    Optional<Cart> findByUserId(Long userId);

    Optional<Cart> deleteByUserId(Long userId);
}
