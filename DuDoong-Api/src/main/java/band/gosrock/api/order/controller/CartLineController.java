package band.gosrock.api.order.controller;


import band.gosrock.api.order.service.CreateCartLineUseCase;
import band.gosrock.api.order.service.ReadCartLineUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "장바구니 관련 컨트롤러")
@RestController
@RequestMapping("/api/v1/cartLines")
@RequiredArgsConstructor
public class CartLineController {

    private final CreateCartLineUseCase createOrderLineUseCase;
    private final ReadCartLineUseCase readOrderLineUseCase;

    @Operation(summary = "티켓상품의 갯수와 사용자가 입력한 옵션을 가진 orderLine 을 만듭니다.")
    @PostMapping("/ticketItems/{ticketItem_id}")
    public void createOrderLine(@PathVariable("ticketItem_id") Long ticketItemId) {
        createOrderLineUseCase.execute(ticketItemId);
    }

    @Operation(summary = "사용자가 최근에 만들었던 을 불러옵니다. 없으면 data null (구현 안해도 됨)")
    @PostMapping("/ticketItems/recent")
    public void createOrder() {
        readOrderLineUseCase.getRecentOrderLine();
    }
}
