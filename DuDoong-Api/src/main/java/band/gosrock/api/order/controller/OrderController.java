package band.gosrock.api.order.controller;


import band.gosrock.api.order.service.CancelOrderUseCase;
import band.gosrock.api.order.service.ConfirmOrderUseCase;
import band.gosrock.api.order.service.CreateOrderUseCase;
import band.gosrock.api.order.service.ReadOrderUseCase;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.ConfirmPaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "주문 관련 컨트롤러")
@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final ConfirmOrderUseCase confirmOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final ReadOrderUseCase readOrderUseCase;

    @Operation(summary = "주문서를 생성합니다. orderId를 발급받을 수 있습니다.")
    @PostMapping("/create")
    public PaymentsResponse createOrder() {
        return createOrderUseCase.execute();
    }

    @Operation(summary = "결제 승인요청 . successUrl 로 돌아온 웹페이지에서 query 로 받은 응답값을 응답값만 알고계시면 됩니다.")
    @PostMapping("/confirm")
    public PaymentsResponse confirmOrder(
            @RequestBody ConfirmPaymentsRequest confirmPaymentsRequest) {
        return confirmOrderUseCase.execute(confirmPaymentsRequest);
    }

    @Operation(summary = "결제 취소요청. 취소 요청의 주체는 주문 본인, 호스트 관리자.")
    @PostMapping("/{order_id}/cancel")
    public void cancelOrder(@PathVariable("order_id") String orderId) {
        cancelOrderUseCase.execute();
    }

    @Operation(summary = "결제 조회. 결제 조회 권한은 주문 본인,  호스트 관리자.")
    @GetMapping("/{order_id}")
    public void readOrder(@PathVariable("order_id") String orderId) {
        readOrderUseCase.execute();
    }
}
