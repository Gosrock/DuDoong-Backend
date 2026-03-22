package band.gosrock.api.refund.controller

import band.gosrock.api.common.page.PageResponse
import band.gosrock.api.refund.dto.response.RefundResponse
import band.gosrock.api.refund.service.CompleteRefundUseCase
import band.gosrock.api.refund.service.GetRefundsUseCase
import band.gosrock.common.annotation.CurrentUserId
import band.gosrock.domain.domains.order.domain.RefundStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = "7-1. [환불관리]")
@RestController
@RequestMapping("/api/v1/events/{eventId}/refunds")
class RefundController(
    private val getRefundsUseCase: GetRefundsUseCase,
    private val completeRefundUseCase: CompleteRefundUseCase,
) {

    @Operation(summary = "환불 목록 조회")
    @GetMapping
    fun getRefunds(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @RequestParam(required = false) refundStatus: RefundStatus?,
        @ParameterObject pageable: Pageable,
    ): PageResponse<RefundResponse> =
        getRefundsUseCase.execute(userId, eventId, refundStatus, pageable)

    @Operation(summary = "환불 상세 조회")
    @GetMapping("/{orderUuid}")
    fun getRefundDetail(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @PathVariable orderUuid: String,
    ): RefundResponse =
        getRefundsUseCase.getDetail(userId, eventId, orderUuid)

    @Operation(summary = "환불 확인 처리 (REFUND_COMPLETED)")
    @PatchMapping("/{orderUuid}/complete")
    fun completeRefund(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @PathVariable orderUuid: String,
    ): RefundResponse =
        completeRefundUseCase.execute(userId, eventId, orderUuid)
}
