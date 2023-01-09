package band.gosrock.api.cart.service;


import band.gosrock.api.cart.model.dto.AddCartRequest;
import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.cart.domain.CartLineItem;
import band.gosrock.domain.domains.cart.service.CartDomainService;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateCartLinesUseCase {

    private final CartDomainService cartDomainService;
    public void execute(AddCartRequest addCartRequest) {
        List<CartLineItem> cartLines = addCartRequest.getCartLines(
            SecurityUtils.getCurrentUserId());

    }
}
