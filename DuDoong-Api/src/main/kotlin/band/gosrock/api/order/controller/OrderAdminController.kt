package band.gosrock.api.order.controller

import band.gosrock.api.common.page.PageResponse
import band.gosrock.api.order.docs.ApproveOrderExceptionDocs
import band.gosrock.api.order.docs.CancelOrderExceptionDocs
import band.gosrock.api.order.model.dto.request.AdminOrderTableQueryRequest
import band.gosrock.api.order.model.dto.response.OrderAdminTableElement
import band.gosrock.api.order.model.dto.response.OrderResponse
import band.gosrock.api.order.service.ApproveOrderUseCase
import band.gosrock.api.order.service.CancelOrderUseCase
import band.gosrock.api.order.service.ReadOrderUseCase
import band.gosrock.api.order.service.RefuseOrderUseCase
import band.gosrock.common.annotation.ApiErrorExceptionsExample
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = "6-2. [이벤트관리] 주문관리 ")
@RestController
@RequestMapping("/api/v1/events/{eventId}/orders")
class OrderAdminController(
    private val approveOrderUseCase: ApproveOrderUseCase,
    private val readOrderUseCase: ReadOrderUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val refuseOrderUseCase: RefuseOrderUseCase,
) {
    @Operation(summary = "어드민 목록 내 테이블 조회 OrderStage 는 꼭 보내주삼!")
    @GetMapping
    fun getEventOrders(
        @ParameterObject @Valid adminOrderTableQueryRequest: AdminOrderTableQueryRequest,
        @ParameterObject pageable: Pageable,
        @PathVariable eventId: Long,
    ): PageResponse<OrderAdminTableElement> =
        readOrderUseCase.getEventOrders(eventId, adminOrderTableQueryRequest, pageable)

    @Operation(summary = "결제 취소요청. 호스트 관리자가 결제를 취소 시킵니다.! (호스트 관리자용(관리자쪽에서 사용))")
    @ApiErrorExceptionsExample(CancelOrderExceptionDocs::class)
    @PostMapping("/{order_uuid}/cancel")
    fun cancelOrder(
        @PathVariable("eventId") eventId: Long,
        @PathVariable("order_uuid") orderUuid: String,
    ): OrderResponse = cancelOrderUseCase.execute(eventId, orderUuid)

    @Operation(summary = "주문 승인하기 . 호스트 관리자가 티켓 주문을 승인합니다.")
    @ApiErrorExceptionsExample(ApproveOrderExceptionDocs::class)
    @PostMapping("/{order_uuid}/approve")
    fun confirmOrder(
        @PathVariable eventId: Long,
        @PathVariable("order_uuid") orderUuid: String,
    ): OrderResponse = approveOrderUseCase.execute(eventId, orderUuid)

    @Operation(summary = "승인 주문 거절하기 . 호스트 관리자가 승인 대기중인 주문을 거절합니다.")
    @PostMapping("/{order_uuid}/refuse")
    fun refuseOrder(
        @PathVariable eventId: Long,
        @PathVariable("order_uuid") orderUuid: String,
    ): OrderResponse = refuseOrderUseCase.execute(eventId, orderUuid)

    @Operation(summary = "주문관리 리스트 페이지에서 주문 상세정보 조회할때")
    @GetMapping("/{order_uuid}")
    fun getEventOrderDetail(
        @PathVariable eventId: Long,
        @PathVariable("order_uuid") orderUuid: String,
    ): OrderResponse = readOrderUseCase.getEventOrderDetail(eventId, orderUuid)
}
