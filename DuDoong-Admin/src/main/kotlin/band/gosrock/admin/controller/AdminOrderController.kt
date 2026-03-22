package band.gosrock.admin.controller

import band.gosrock.admin.model.dto.request.AdminCancelOrderRequest
import band.gosrock.admin.model.dto.request.AdminRefundStatusRequest
import band.gosrock.admin.model.dto.response.AdminOrderResponse
import band.gosrock.admin.service.AdminCancelOrderUseCase
import band.gosrock.admin.service.AdminExcelService
import band.gosrock.admin.service.AdminGetOrderDetailUseCase
import band.gosrock.admin.service.AdminGetOrdersUseCase
import band.gosrock.admin.service.AdminUpdateRefundStatusUseCase
import band.gosrock.common.annotation.CurrentUserId
import band.gosrock.domain.domains.order.domain.OrderStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal-api/v1/orders")
@SecurityRequirement(name = "admin-token")
@Tag(name = "Admin")
class AdminOrderController(
    private val adminGetOrdersUseCase: AdminGetOrdersUseCase,
    private val adminGetOrderDetailUseCase: AdminGetOrderDetailUseCase,
    private val adminCancelOrderUseCase: AdminCancelOrderUseCase,
    private val adminUpdateRefundStatusUseCase: AdminUpdateRefundStatusUseCase,
    private val adminExcelService: AdminExcelService,
) {

    @Operation(summary = "주문 목록을 엑셀로 다운로드합니다.")
    @GetMapping("/export")
    fun exportOrders(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: OrderStatus?,
        @RequestParam(required = false) eventId: Long?,
    ): ResponseEntity<ByteArray> {
        val orders = adminGetOrdersUseCase.executeAll(userId, keyword, status, eventId)
        val bytes = adminExcelService.generateOrdersExcel(orders)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(bytes)
    }

    @Operation(summary = "주문 목록을 조회합니다.")
    @GetMapping
    fun getOrders(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: OrderStatus?,
        @RequestParam(required = false) eventId: Long?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<AdminOrderResponse> {
        return adminGetOrdersUseCase.execute(userId, keyword, status, eventId, pageable)
    }

    @Operation(summary = "주문 상세 정보를 조회합니다.")
    @GetMapping("/{orderUuid}")
    fun getOrderDetail(@CurrentUserId userId: Long, @PathVariable orderUuid: String): AdminOrderResponse {
        return adminGetOrderDetailUseCase.execute(userId, orderUuid)
    }

    @Operation(summary = "주문을 취소합니다.")
    @PostMapping("/{orderUuid}/cancel")
    fun cancelOrder(
        @CurrentUserId userId: Long,
        @PathVariable orderUuid: String,
        @RequestBody(required = false) request: AdminCancelOrderRequest?,
    ): AdminOrderResponse {
        return adminCancelOrderUseCase.execute(userId, orderUuid, request?.reason)
    }

    @Operation(summary = "주문의 환불 상태를 변경합니다. (REFUND_COMPLETED)")
    @PatchMapping("/{orderUuid}/refund-status")
    fun updateRefundStatus(
        @CurrentUserId userId: Long,
        @PathVariable orderUuid: String,
        @RequestBody @Valid request: AdminRefundStatusRequest,
    ): AdminOrderResponse {
        return adminUpdateRefundStatusUseCase.execute(userId, orderUuid, request)
    }
}
