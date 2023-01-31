package band.gosrock.api.order.controller;


import band.gosrock.api.common.page.PageResponse;
import band.gosrock.api.order.docs.ApproveOrderExceptionDocs;
import band.gosrock.api.order.docs.CancelOrderExceptionDocs;
import band.gosrock.api.order.docs.ConfirmOrderExceptionDocs;
import band.gosrock.api.order.docs.CreateOrderExceptionDocs;
import band.gosrock.api.order.docs.FreeOrderExceptionDocs;
import band.gosrock.api.order.docs.RefundOrderExceptionDocs;
import band.gosrock.api.order.model.dto.request.AdminOrderTableQueryRequest;
import band.gosrock.api.order.model.dto.request.ConfirmOrderRequest;
import band.gosrock.api.order.model.dto.request.CreateOrderRequest;
import band.gosrock.api.order.model.dto.response.CreateOrderResponse;
import band.gosrock.api.order.model.dto.response.OrderBriefElement;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.api.order.service.ApproveOrderUseCase;
import band.gosrock.api.order.service.CancelOrderUseCase;
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
@Tag(name = "주문 관련 컨트롤러")
@RestController
@RequestMapping("/v1/events/{eventId}/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final ApproveOrderUseCase approveOrderUseCase;
    private final ReadOrderUseCase readOrderUseCase;



    //TODO : 어프로브 주문 이동처리
    @Operation(summary = "어드민 목록 내 테이블 조회 OrderStage 는 꼭 보내주삼!")
    @GetMapping
    public PageResponse<OrderResponse> getEventOrders(
        @ParameterObject @Valid AdminOrderTableQueryRequest adminOrderTableQueryRequest) {
        return null;
//        return readOrderUseCase.getEventOrders(adminOrderTableQueryRequest, pageable);
    }
}
