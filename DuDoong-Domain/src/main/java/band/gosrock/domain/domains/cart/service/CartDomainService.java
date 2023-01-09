package band.gosrock.domain.domains.cart.service;

import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CartDomainService {

    private final CartRepository cartRepository;
}
