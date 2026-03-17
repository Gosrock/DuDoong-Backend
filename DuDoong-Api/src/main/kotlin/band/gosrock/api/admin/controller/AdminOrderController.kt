package band.gosrock.api.admin.controller

import band.gosrock.api.admin.model.dto.response.AdminOrderResponse
import band.gosrock.api.admin.service.AdminCancelOrderUseCase
import band.gosrock.api.admin.service.AdminGetOrderDetailUseCase
import band.gosrock.api.admin.service.AdminGetOrdersUseCase
import band.gosrock.domain.domains.order.domain.OrderStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal-api/v1/orders")
@SecurityRequirement(name = "access-token")
@Tag(name = "Admin")
class AdminOrderController(
    private val adminGetOrdersUseCase: AdminGetOrdersUseCase,
    private val adminGetOrderDetailUseCase: AdminGetOrderDetailUseCase,
    private val adminCancelOrderUseCase: AdminCancelOrderUseCase,
) {

    @Operation(summary = "주문 목록을 조회합니다.")
    @GetMapping
    fun getOrders(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: OrderStatus?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<AdminOrderResponse> {
        return adminGetOrdersUseCase.execute(keyword, status, pageable)
    }

    @Operation(summary = "주문 상세 정보를 조회합니다.")
    @GetMapping("/{orderUuid}")
    fun getOrderDetail(@PathVariable orderUuid: String): AdminOrderResponse {
        return adminGetOrderDetailUseCase.execute(orderUuid)
    }

    @Operation(summary = "주문을 취소합니다.")
    @PostMapping("/{orderUuid}/cancel")
    fun cancelOrder(@PathVariable orderUuid: String): AdminOrderResponse {
        return adminCancelOrderUseCase.execute(orderUuid)
    }
}
