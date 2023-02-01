package band.gosrock.api.order.controller;


import band.gosrock.api.common.page.PageResponse;
import band.gosrock.api.order.docs.ApproveOrderExceptionDocs;
import band.gosrock.api.order.model.dto.request.AdminOrderTableQueryRequest;
import band.gosrock.api.order.model.dto.response.OrderAdminTableElement;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.api.order.service.ApproveOrderUseCase;
import band.gosrock.api.order.service.ReadOrderUseCase;
import band.gosrock.common.annotation.ApiErrorExceptionsExample;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "주문 관련 컨트롤러")
@RestController
@RequestMapping("/v1/events/{eventId}/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final ApproveOrderUseCase approveOrderUseCase;
    private final ReadOrderUseCase readOrderUseCase;

    @Operation(summary = "어드민 목록 내 테이블 조회 OrderStage 는 꼭 보내주삼!")
    @GetMapping
    public PageResponse<OrderAdminTableElement> getEventOrders(
            @ParameterObject @Valid AdminOrderTableQueryRequest adminOrderTableQueryRequest,
            @ParameterObject Pageable pageable,
            @PathVariable Long eventId) {
        return readOrderUseCase.getEventOrders(eventId, adminOrderTableQueryRequest, pageable);
    }

    @Operation(summary = "주문 승인하기 . 호스트 관리자가 티켓 주문을 승인합니다. ( 어드민 이벤트쪽으로 이동예정 )")
    @ApiErrorExceptionsExample(ApproveOrderExceptionDocs.class)
    @PostMapping("/{order_uuid}/approve")
    public OrderResponse confirmOrder(
            @PathVariable Long eventId, @PathVariable("order_uuid") String orderUuid) {
        return approveOrderUseCase.execute(eventId, orderUuid);
    }
}
