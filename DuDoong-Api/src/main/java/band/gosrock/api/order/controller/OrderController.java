package band.gosrock.api.order.controller;


import band.gosrock.api.order.model.dto.request.ConfirmOrderRequest;
import band.gosrock.api.order.model.dto.request.CreateOrderRequest;
import band.gosrock.api.order.model.dto.response.CreateOrderResponse;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.api.order.service.CancelOrderUseCase;
import band.gosrock.api.order.service.ConfirmOrderUseCase;
import band.gosrock.api.order.service.CreateOrderUseCase;
import band.gosrock.api.order.service.CreateTossOrderUseCase;
import band.gosrock.api.order.service.ReadOrderUseCase;
import band.gosrock.common.annotation.DevelopOnlyApi;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
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

    private final CreateTossOrderUseCase createTossOrderUseCase;

    @Operation(summary = "토스페이먼츠에서 주문서를 생성합니다.(테스트용)")
    @DevelopOnlyApi
    @PostMapping("/testToss/{order_id}")
    public PaymentsResponse createTossOrderUseCase(@PathVariable("order_id") String orderId) {
        return createTossOrderUseCase.execute(orderId);
    }

    // TODO : 승인 결제 방식 도입하면서 좀더 이쁘게 만들 예정
    @Operation(summary = "카드 주문을 생성합니다. 쿠폰없을때 쿠폰아이디 null 로 보내주세염!")
    @PostMapping("/card")
    public CreateOrderResponse createOrder(
            @RequestBody @Valid CreateOrderRequest createOrderRequest) {
        return createOrderUseCase.execute(createOrderRequest);
    }

    @Operation(summary = "결제 승인요청 . successUrl 로 돌아온 웹페이지에서 query 로 받은 응답값을 서버로 보냅니당.")
    @PostMapping("/{order_id}/confirm")
    public OrderResponse confirmOrder(
            @PathVariable("order_id") String orderId,
            @RequestBody ConfirmOrderRequest confirmOrderRequest) {
        return confirmOrderUseCase.execute(orderId, confirmOrderRequest);
    }

    @Operation(summary = "결제 취소요청. 취소 요청의 주체는 주문 본인, 호스트 관리자.")
    @PostMapping("/{order_id}/cancel")
    public void cancelOrder(@PathVariable("order_id") String orderId) {
        cancelOrderUseCase.execute();
    }

    @Operation(summary = "결제 조회. 결제 조회 권한은 주문 본인,  호스트 관리자.")
    @GetMapping("/{order_id}")
    public OrderResponse readOrder(@PathVariable("order_id") String orderId) {
        return readOrderUseCase.execute(orderId);
    }
}
