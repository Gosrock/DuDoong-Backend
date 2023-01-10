package band.gosrock.api.cart.controller;


import band.gosrock.api.cart.model.dto.request.AddCartRequest;
import band.gosrock.api.cart.model.dto.response.CreateCartResponse;
import band.gosrock.api.cart.service.CreateCartUseCase;
import band.gosrock.api.cart.service.ReadCartLineUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "장바구니 관련 컨트롤러")
@RestController
@RequestMapping("/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CreateCartUseCase createCartUseCase;
    private final ReadCartLineUseCase readOrderLineUseCase;

    //    @Operation(summary = "상품 아이디에 답변을 해야하는 옵션이 있는지 확인합니다.(추후 아이템 도메인으로 이전?)")
    //    @GetMapping("/check/answer")
    //    public void createCartLines() {
    //        createOrderLineUseCase.execute(ticketItemId);
    //    }
    @Operation(summary = "상품을 장바구니에 담습니다. 상품에 답변해야하는 응답이 있다면, 응답도 보내주시면 됩니다.")
    @PostMapping
    public CreateCartResponse createCartLines(@RequestBody @Valid AddCartRequest addCartRequest) {
        return createCartUseCase.execute(addCartRequest);
    }

    //    @Operation(summary = "내 장바구니에 담긴 상품들의 목록을 불러옵니다.")
    //    @GetMapping
    //    public void createCartLines() {
    //        createOrderLineUseCase.execute(ticketItemId);
    //    }

    //    @Operation(summary = "장바구니에 담은 상품의 정보를 가져옵니다. 옵션이있으면 옵션마다 다른 라인을 가집니다.")
    //    @GetMapping("/lines/{line_id}")
    //    public void getCartLineInfo(@PathVariable("line_id") Long ticketItemId) {
    //        createOrderLineUseCase.execute(ticketItemId);
    //    }

    //    @Operation(summary = "장바구니에 담은 상품의 옵션을")
    //    @GetMapping("/{cart_id}/lines/{line_id}/option")
    //    public void getItemOptions(@PathVariable("cart_id") Long cartId,@PathVariable("line_id")
    // Long ticketItemId) {
    //        createOrderLineUseCase.execute(ticketItemId);
    //    }

    //    @Operation(summary = "티켓상품의 갯수와 사용자가 입력한 옵션을 가진 orderLine 을 만듭니다.")
    //    @PostMapping("/option")
    //    public void createOrderLine(@PathVariable("line_id") Long ticketItemId) {
    //        createOrderLineUseCase.execute(ticketItemId);
    //    }
    @Operation(summary = "사용자가 최근에 만들었던 장바구니를 불러옵니다. 없으면 data null (구현 안해도 됨)")
    @PostMapping("/recent")
    public void createOrder() {
        readOrderLineUseCase.getRecentOrderLine();
    }
}
