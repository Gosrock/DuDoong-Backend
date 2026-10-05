package band.gosrock.api.v2.order.controller

import band.gosrock.api.v2.common.swagger.V2ApiTags
import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.order.dto.V2MyOrderStatusFilter
import band.gosrock.api.v2.order.dto.request.V2CancelMyOrderRequest
import band.gosrock.api.v2.order.dto.request.V2CreateOrderRequest
import band.gosrock.api.v2.order.dto.request.V2RefundAccountRequest
import band.gosrock.api.v2.order.dto.response.V2CheckoutResponse
import band.gosrock.api.v2.order.dto.response.V2MyOrderDetailResponse
import band.gosrock.api.v2.order.dto.response.V2MyOrderElement
import band.gosrock.api.v2.order.usecase.V2CancelMyOrderUseCase
import band.gosrock.api.v2.order.usecase.V2CreateOrderUseCase
import band.gosrock.api.v2.order.usecase.V2ReadCheckoutUseCase
import band.gosrock.api.v2.order.usecase.V2PutRefundAccountUseCase
import band.gosrock.api.v2.order.usecase.V2ReadMyOrdersUseCase
import band.gosrock.common.annotation.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = V2ApiTags.ORDER, description = V2ApiTags.ORDER_DESCRIPTION)
@RestController
@RequestMapping("/api/v2")
@Validated
class V2OrderController(
    private val createOrderUseCase: V2CreateOrderUseCase,
    private val readMyOrdersUseCase: V2ReadMyOrdersUseCase,
    private val cancelMyOrderUseCase: V2CancelMyOrderUseCase,
    private val readCheckoutUseCase: V2ReadCheckoutUseCase,
    private val putRefundAccountUseCase: V2PutRefundAccountUseCase,
) {
    @Operation(
        summary = "[O-0] 결제 화면 (로그인). 티켓(P-5 와 같음) + 입금 계좌·예금주(두둥티켓 + 구매 가능할 때만, 무료·지난 공연·정산중·종료·매진은 null). " +
            "[결제하기]·토스 송금 전에 보여 줄 값이며, 공개 P-5 에는 계좌가 없다. 판매 중이 아니거나 다른 공연 티켓이면 404",
    )
    @GetMapping("/events/{eventId}/ticket-items/{ticketItemId}/checkout")
    fun getCheckout(
        // 로그인한 사용자에게만 계좌를 준다 (SecurityConfig 공개 경로에 넣지 않음). 값 자체는 쓰지 않는다
        @Suppress("UNUSED_PARAMETER") @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @PathVariable ticketItemId: Long,
    ): V2CheckoutResponse = readCheckoutUseCase.execute(eventId, ticketItemId)

    @Operation(
        summary = "[O-1] 주문 생성 (로그인). 두둥티켓은 '입금했어요' 시점에 승인 대기로, 무료는 승인 ON 이면 승인 대기·OFF 면 즉시 발급. " +
            "같은 사용자의 같은 주문이 10초 안에 다시 오면 앞 주문을 돌려준다. 응답은 O-3 과 같은 주문 상세",
    )
    @PostMapping("/orders")
    fun createOrder(@CurrentUserId userId: Long, @RequestBody @Valid request: V2CreateOrderRequest): V2MyOrderDetailResponse =
        createOrderUseCase.execute(userId, request)

    @Operation(summary = "[O-2] 내 주문 목록 (로그인). 최신 순, status 기본 ALL")
    @GetMapping("/me/orders")
    fun getMyOrders(
        @CurrentUserId userId: Long,
        @RequestParam(defaultValue = "ALL") status: V2MyOrderStatusFilter,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) size: Int,
    ): V2PageResponse<V2MyOrderElement> = readMyOrdersUseCase.execute(userId, status.domain, page, size)

    @Operation(summary = "[O-3] 내 주문 상세 (로그인, 본인 주문만 — 남의 주문은 404). 거절 사유, 결제 계좌(유료), 본인 발급 티켓, canCancel")
    @GetMapping("/me/orders/{orderUuid}")
    fun getMyOrder(@CurrentUserId userId: Long, @PathVariable orderUuid: String): V2MyOrderDetailResponse =
        readMyOrdersUseCase.detail(userId, orderUuid)

    @Operation(
        summary = "[O-4] 취소·환불 요청 (로그인, 본인 주문). 승인 대기: 공연 시작 전, 승인 완료: 공연 시작 전 + 입장한 티켓 없음. " +
            "유료는 환불 계좌 필수(호스트가 송금 후 환불 완료 처리). 이미 취소된 주문은 Order_400_5",
    )
    @PostMapping("/me/orders/{orderUuid}/cancel")
    fun cancelMyOrder(
        @CurrentUserId userId: Long,
        @PathVariable orderUuid: String,
        @RequestBody(required = false) @Valid request: V2CancelMyOrderRequest?,
    ): V2MyOrderDetailResponse = cancelMyOrderUseCase.execute(userId, orderUuid, request)

    @Operation(
        summary = "[O-5] 환불 계좌 입력·수정 (로그인, 본인 주문 — 남의 주문은 404). v2 주문 중 환불 요청 중인 유료 계좌이체 주문(호스트 거절·호스트 취소·사용자 취소)만. " +
            "대상 아님(v1 주문·무료·카드·환불 요청 없음)은 Order_400_27, 대상 주문의 환불 완료 뒤는 Order_400_28. 검증은 O-4 계좌와 같음. " +
            "이미 있던 계좌를 바꾸면 호스트 마스터·매니저에게 REFUND_ACCOUNT_CHANGED 알림. 응답은 O-3 주문 상세",
    )
    @PutMapping("/me/orders/{orderUuid}/refund-account")
    fun putRefundAccount(
        @CurrentUserId userId: Long,
        @PathVariable orderUuid: String,
        @RequestBody @Valid request: V2RefundAccountRequest,
    ): V2MyOrderDetailResponse = putRefundAccountUseCase.execute(userId, orderUuid, request)

    companion object {
        const val MAX_PAGE_SIZE = 50L
    }
}
