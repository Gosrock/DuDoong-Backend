package band.gosrock.api.cart.controller;


import band.gosrock.api.cart.docs.CreateCartExceptionDocs;
import band.gosrock.api.cart.model.dto.request.AddCartRequest;
import band.gosrock.api.cart.model.dto.response.CartResponse;
import band.gosrock.api.cart.service.CreateCartUseCase;
import band.gosrock.api.cart.service.ReadCartUseCase;
import band.gosrock.common.annotation.ApiErrorExceptionsExample;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "5. 장바구니 관련 컨트롤러")
@RestController
@RequestMapping("/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CreateCartUseCase createCartUseCase;
    private final ReadCartUseCase readCartUseCase;

    @Operation(summary = "상품을 장바구니에 담습니다. 상품에 답변해야하는 응답이 있다면, 응답도 보내주시면 됩니다.")
    @ApiErrorExceptionsExample(CreateCartExceptionDocs.class)
    @PostMapping
    public CartResponse createCartLines(@RequestBody @Valid AddCartRequest addCartRequest) {
        return createCartUseCase.execute(addCartRequest);
    }

    @Operation(summary = "사용자가 최근에 만들었던 장바구니를 불러옵니다. 없으면 data null (구현 안해도 됨)")
    @GetMapping("/recent")
    public CartResponse getRecentMyCart() {
        return readCartUseCase.execute();
    }
}
