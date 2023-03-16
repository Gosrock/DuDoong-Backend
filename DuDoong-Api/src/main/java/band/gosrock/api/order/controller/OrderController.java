package band.gosrock.api.order.controller;


import band.gosrock.api.common.slice.SliceResponse;
import band.gosrock.api.order.docs.ConfirmOrderExceptionDocs;
import band.gosrock.api.order.docs.CreateOrderExceptionDocs;
import band.gosrock.api.order.docs.FreeOrderExceptionDocs;
import band.gosrock.api.order.docs.RefundOrderExceptionDocs;
import band.gosrock.api.order.model.dto.request.ConfirmOrderRequest;
import band.gosrock.api.order.model.dto.request.CreateOrderRequest;
import band.gosrock.api.order.model.dto.response.CreateOrderResponse;
import band.gosrock.api.order.model.dto.response.OrderBriefElement;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.api.order.model.dto.response.OrderTicketResponse;
import band.gosrock.api.order.service.ConfirmOrderUseCase;
import band.gosrock.api.order.service.CreateOrderUseCase;
import band.gosrock.api.order.service.CreateTossOrderUseCase;
import band.gosrock.api.order.service.FreeOrderUseCase;
import band.gosrock.api.order.service.ReadOrderUseCase;
import band.gosrock.api.order.service.RefundOrderUseCase;
import band.gosrock.common.annotation.ApiErrorExceptionsExample;
import band.gosrock.common.annotation.DevelopOnlyApi;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "6-1. [주문]")
@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final ConfirmOrderUseCase confirmOrderUseCase;
    private final FreeOrderUseCase freeOrderUseCase;

    private final RefundOrderUseCase refundOrderUseCase;
    private final ReadOrderUseCase readOrderUseCase;

    private final CreateTossOrderUseCase createTossOrderUseCase;

    @Operation(summary = "토스페이먼츠에서 주문서를 생성합니다.(테스트용)")
    @DevelopOnlyApi
    @PostMapping("/testToss/{order_uuid}")
    public PaymentsResponse createTossOrderUseCase(@PathVariable("order_uuid") String orderUuid) {
        return createTossOrderUseCase.execute(orderUuid);
    }

    @Operation(summary = "주문을 생성합니다. 장바구니 아이디를 주문서로 변환하는 작업을 합니다.")
    @ApiErrorExceptionsExample(CreateOrderExceptionDocs.class)
    @PostMapping("/")
    public CreateOrderResponse createOrder(
            @RequestBody @Valid CreateOrderRequest createOrderRequest) {
        return createOrderUseCase.execute(createOrderRequest);
    }

    @Operation(summary = "결제 확인하기 . successUrl 로 돌아온 웹페이지에서 query 로 받은 응답값을 서버로 보냅니당.")
    @ApiErrorExceptionsExample(ConfirmOrderExceptionDocs.class)
    @PostMapping("/{order_uuid}/confirm")
    public OrderResponse confirmOrder(
            @PathVariable("order_uuid") String orderUuid,
            @RequestBody ConfirmOrderRequest confirmOrderRequest) {
        return confirmOrderUseCase.execute(orderUuid, confirmOrderRequest);
    }

    @Operation(summary = "주문을 무료로 결제합니다. 선착순 방식 결제 0원일 때 지원")
    @ApiErrorExceptionsExample(FreeOrderExceptionDocs.class)
    @PostMapping("/{order_uuid}/free")
    public OrderResponse freeOrder(@PathVariable("order_uuid") String orderUuid) {
        return freeOrderUseCase.execute(orderUuid);
    }

    @Operation(summary = "결제 환불요청. 본인이 구매한 오더를 환불 시킵니다.! (본인 용)")
    @ApiErrorExceptionsExample(RefundOrderExceptionDocs.class)
    @PostMapping("/{order_uuid}/refund")
    public OrderResponse refundOrder(@PathVariable("order_uuid") String orderUuid) {
        return refundOrderUseCase.execute(orderUuid);
    }

    @Operation(summary = "결제 조회. 결제 조회 권한은 주문 본인")
    @GetMapping("/{order_uuid}")
    public OrderResponse getOrderDetail(@PathVariable("order_uuid") String orderUuid) {
        return readOrderUseCase.getOrderDetail(orderUuid);
    }

    @Operation(summary = "결제 아이디로 발급된 티켓 조회")
    @GetMapping("/{order_uuid}/tickets")
    public OrderTicketResponse getOrderTickets(@PathVariable("order_uuid") String orderUuid) {
        return readOrderUseCase.getOrderTickets(orderUuid);
    }

    @Operation(summary = "최근 예매내역 조회")
    @GetMapping("/recent")
    public OrderBriefElement getRecentOrder() {
        return readOrderUseCase.getRecentOrder();
    }

    @Operation(summary = "마이페이지 내 예매목록 조회")
    @GetMapping
    public SliceResponse<OrderBriefElement> getMyOrders(
            @ParameterObject @RequestParam Boolean showing,
            @ParameterObject @PageableDefault Pageable pageable) {
        return readOrderUseCase.getMyOrders(showing, pageable);
    }
}
