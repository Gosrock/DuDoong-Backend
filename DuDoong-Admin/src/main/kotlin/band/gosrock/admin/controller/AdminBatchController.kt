package band.gosrock.admin.controller

import band.gosrock.admin.model.dto.response.AdminBatchExecutionResponse
import band.gosrock.admin.model.dto.response.AdminBatchJobSummaryResponse
import band.gosrock.admin.service.AdminGetBatchExecutionsUseCase
import band.gosrock.admin.service.AdminGetBatchJobsUseCase
import band.gosrock.common.annotation.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal-api/v1/batch")
@SecurityRequirement(name = "admin-token")
@Tag(name = "Admin")
class AdminBatchController(
    private val adminGetBatchJobsUseCase: AdminGetBatchJobsUseCase,
    private val adminGetBatchExecutionsUseCase: AdminGetBatchExecutionsUseCase,
) {

    @Operation(summary = "배치 잡별 최근 실행 상태를 조회합니다. (마지막 실행, 마지막 성공, 최근 7일 실패 수)")
    @GetMapping("/jobs")
    fun getJobs(@CurrentUserId userId: Long): List<AdminBatchJobSummaryResponse> {
        return adminGetBatchJobsUseCase.execute(userId)
    }

    @Operation(summary = "배치 실행 이력을 최신순으로 조회합니다. (jobName 정확히 일치 필터, size 최대 100)")
    @GetMapping("/executions")
    fun getExecutions(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) jobName: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<AdminBatchExecutionResponse> {
        return adminGetBatchExecutionsUseCase.execute(userId, jobName, pageable)
    }
}
