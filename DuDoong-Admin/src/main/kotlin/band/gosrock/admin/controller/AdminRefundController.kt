package band.gosrock.admin.controller

import band.gosrock.admin.model.dto.response.AdminRefundResponse
import band.gosrock.admin.service.AdminCompleteRefundUseCase
import band.gosrock.admin.service.AdminGetRefundDetailUseCase
import band.gosrock.admin.service.AdminGetRefundsUseCase
import band.gosrock.common.annotation.CurrentUserId
import band.gosrock.domain.domains.order.domain.RefundStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal-api/v1/refunds")
@SecurityRequirement(name = "admin-token")
@Tag(name = "Admin")
class AdminRefundController(
    private val adminGetRefundsUseCase: AdminGetRefundsUseCase,
    private val adminGetRefundDetailUseCase: AdminGetRefundDetailUseCase,
    private val adminCompleteRefundUseCase: AdminCompleteRefundUseCase,
) {

    @Operation(summary = "전체 환불 목록을 조회합니다.")
    @GetMapping
    fun getRefunds(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) refundStatus: RefundStatus?,
        @RequestParam(required = false) eventId: Long?,
        @RequestParam(required = false) keyword: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<AdminRefundResponse> =
        adminGetRefundsUseCase.execute(userId, refundStatus, eventId, keyword, pageable)

    @Operation(summary = "환불 상세 정보를 조회합니다.")
    @GetMapping("/{orderUuid}")
    fun getRefundDetail(
        @CurrentUserId userId: Long,
        @PathVariable orderUuid: String,
    ): AdminRefundResponse =
        adminGetRefundDetailUseCase.execute(userId, orderUuid)

    @Operation(summary = "환불을 확인 처리합니다. (REFUND_COMPLETED)")
    @PatchMapping("/{orderUuid}/complete")
    fun completeRefund(
        @CurrentUserId userId: Long,
        @PathVariable orderUuid: String,
    ): AdminRefundResponse =
        adminCompleteRefundUseCase.execute(userId, orderUuid)
}
