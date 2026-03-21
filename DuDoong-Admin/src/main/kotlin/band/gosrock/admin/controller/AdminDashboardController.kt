package band.gosrock.admin.controller

import band.gosrock.admin.model.dto.response.DashboardResponse
import band.gosrock.admin.service.GetDashboardUseCase
import band.gosrock.common.annotation.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal-api/v1")
@SecurityRequirement(name = "admin-token")
@Tag(name = "Admin")
class AdminDashboardController(
    private val getDashboardUseCase: GetDashboardUseCase,
) {

    @Operation(summary = "어드민 대시보드 통계를 조회합니다.")
    @GetMapping("/dashboard")
    fun getDashboard(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
    ): DashboardResponse {
        return getDashboardUseCase.execute(userId, startDate, endDate)
    }
}
