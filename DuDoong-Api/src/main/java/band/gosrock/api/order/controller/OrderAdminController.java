package band.gosrock.api.order.controller;


import band.gosrock.api.common.page.PageResponse;
import band.gosrock.api.order.model.dto.request.AdminOrderTableQueryRequest;
import band.gosrock.api.order.model.dto.response.OrderAdminTableElement;
import band.gosrock.api.order.service.ApproveOrderUseCase;
import band.gosrock.api.order.service.ReadOrderUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    // TODO : 어프로브 주문 이동처리
    @Operation(summary = "어드민 목록 내 테이블 조회 OrderStage 는 꼭 보내주삼!")
    @GetMapping
    public PageResponse<OrderAdminTableElement> getEventOrders(
            @ParameterObject @Valid AdminOrderTableQueryRequest adminOrderTableQueryRequest,
            @ParameterObject Pageable pageable,
            @PathVariable Long eventId) {
        return readOrderUseCase.getEventOrders(eventId, adminOrderTableQueryRequest, pageable);
    }
}
