package band.gosrock.api.order.controller;


import band.gosrock.api.order.service.CreateOrderLineUseCase;
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
@RequestMapping("/v1/orderLines")
@RequiredArgsConstructor
public class OrderLineController {

    private CreateOrderLineUseCase createOrderLineUseCase;

    @Operation(summary = "티켓상품의 갯수와 사용자가 입력한 옵션을 가진 orderLine 을 만듭니다.")
    @PostMapping("ticketItems/{ticketItem_id}")
    public void createOrder(@PathVariable("ticketItem_id") String ticketItemId) {
        createOrderLineUseCase.execute();
//        return oauthUserInfoUseCase.execute(accessToken);
    }
}
