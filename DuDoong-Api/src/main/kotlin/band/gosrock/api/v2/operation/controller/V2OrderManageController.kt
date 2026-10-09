package band.gosrock.api.v2.operation.controller

import band.gosrock.api.v2.common.swagger.V2ApiTags
import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.operation.dto.V2OrderStatusFilter
import band.gosrock.api.v2.operation.dto.V2RefundStatusFilter
import band.gosrock.api.v2.operation.dto.request.V2CancelOrderRequest
import band.gosrock.api.v2.operation.dto.request.V2RefuseOrderRequest
import band.gosrock.api.v2.operation.dto.response.V2DashboardResponse
import band.gosrock.api.v2.operation.dto.response.V2OrderDetailResponse
import band.gosrock.api.v2.operation.dto.response.V2OrderListResponse
import band.gosrock.api.v2.operation.dto.response.V2RefundElement
import band.gosrock.api.v2.operation.usecase.V2ChangeOrderUseCase
import band.gosrock.api.v2.operation.usecase.V2ReadDashboardUseCase
import band.gosrock.api.v2.operation.usecase.V2ReadOrdersUseCase
import band.gosrock.common.annotation.CurrentUserId
import band.gosrock.domain.domains.order.service.v2.V2OrderSearchType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = V2ApiTags.OPERATION_ORDER, description = V2ApiTags.OPERATION_ORDER_DESCRIPTION)
@RestController
@RequestMapping("/api/v2/events/{eventId}")
@Validated
class V2OrderManageController(
    private val readDashboardUseCase: V2ReadDashboardUseCase,
    private val readOrdersUseCase: V2ReadOrdersUseCase,
    private val changeOrderUseCase: V2ChangeOrderUseCase,
) {
    @Operation(summary = "[D-1] 대시보드 (일반 멤버 이상). 주문 건수·티켓별 판매 매수·판매금액·입장 현황")
    @GetMapping("/dashboard")
    fun getDashboard(@CurrentUserId userId: Long, @PathVariable eventId: Long): V2DashboardResponse =
        readDashboardUseCase.execute(userId, eventId)

    @Operation(summary = "[R-1] 주문 목록 (일반 멤버 이상). 최신 순, 상태별 건수(counts) 포함. searchType: NAME(기본) / PHONE / DEPOSITOR_NAME(입금자명, v2 두둥티켓 주문)")
    @GetMapping("/orders")
    fun getOrders(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @RequestParam(defaultValue = "ALL") status: V2OrderStatusFilter,
        @RequestParam(required = false) searchType: V2OrderSearchType?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) size: Int,
    ): V2OrderListResponse = readOrdersUseCase.execute(userId, eventId, status, searchType, keyword, page, size)

    @Operation(summary = "[R-6] 주문 엑셀 다운로드 (일반 멤버 이상). R-1 과 같은 필터, 전체 행 (xlsx)")
    @GetMapping("/orders/export")
    fun exportOrders(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @RequestParam(defaultValue = "ALL") status: V2OrderStatusFilter,
        @RequestParam(required = false) searchType: V2OrderSearchType?,
        @RequestParam(required = false) keyword: String?,
    ): ResponseEntity<ByteArray> =
        V2Excel.attachment("orders-$eventId.xlsx", readOrdersUseCase.export(userId, eventId, status, searchType, keyword))

    @Operation(summary = "[R-2] 주문 상세 (일반 멤버 이상). 주문 라인·발급 티켓별 옵션 응답. 다른 공연 주문은 404")
    @GetMapping("/orders/{orderUuid}")
    fun getOrder(@CurrentUserId userId: Long, @PathVariable eventId: Long, @PathVariable orderUuid: String): V2OrderDetailResponse =
        readOrdersUseCase.detail(userId, eventId, orderUuid)

    @Operation(summary = "[R-3] 승인 (매니저 이상). 승인 대기 주문만 (아니면 Order_400_3)")
    @PostMapping("/orders/{orderUuid}/approve")
    fun approveOrder(@CurrentUserId userId: Long, @PathVariable eventId: Long, @PathVariable orderUuid: String): V2OrderDetailResponse =
        changeOrderUseCase.approve(userId, eventId, orderUuid)

    @Operation(summary = "[R-4] 거절 (매니저 이상). 상태는 v1 과 같은 CANCELED, v2 목록에서는 REFUSED. 환불 요청은 결제 금액이 있을 때만 (0원 주문은 환불 요청 없음 — F-1 에 안 나옴). 사유 문구는 v1 cancelReason 에도 기록")
    @PostMapping("/orders/{orderUuid}/refuse")
    fun refuseOrder(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @PathVariable orderUuid: String,
        @RequestBody @Valid request: V2RefuseOrderRequest,
    ): V2OrderDetailResponse = changeOrderUseCase.refuse(userId, eventId, orderUuid, request)

    @Operation(summary = "[R-5] 승인 완료 주문 취소 (매니저 이상). 발급 티켓 취소·재고 복구 (v1 과 같은 로직), 환불 요청은 결제 금액이 있을 때만 (0원 주문은 환불 요청 없음 — F-1 에 안 나옴)")
    @PostMapping("/orders/{orderUuid}/cancel")
    fun cancelOrder(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @PathVariable orderUuid: String,
        @RequestBody(required = false) @Valid request: V2CancelOrderRequest?,
    ): V2OrderDetailResponse = changeOrderUseCase.cancel(userId, eventId, orderUuid, request)

    @Operation(summary = "[F-1] 환불 목록 (일반 멤버 이상). status 없으면 요청·완료 전부, 최신 순")
    @GetMapping("/refunds")
    fun getRefunds(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @RequestParam(required = false) status: V2RefundStatusFilter?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) size: Int,
    ): V2PageResponse<V2RefundElement> = readOrdersUseCase.refunds(userId, eventId, status, page, size)

    @Operation(summary = "[F-2] 환불 완료 (매니저 이상, DEC-016). 환불 요청 주문만 (아니면 Order_400_17), 이미 완료면 그대로 200")
    @PostMapping("/refunds/{orderUuid}/complete")
    fun completeRefund(@CurrentUserId userId: Long, @PathVariable eventId: Long, @PathVariable orderUuid: String): V2OrderDetailResponse =
        changeOrderUseCase.completeRefund(userId, eventId, orderUuid)

    companion object {
        const val MAX_PAGE_SIZE = 100L
    }
}
