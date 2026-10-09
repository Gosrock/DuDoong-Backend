package band.gosrock.admin.controller

import band.gosrock.admin.model.dto.response.AdminStagingServerResponse
import band.gosrock.admin.service.AdminGetStagingServerUseCase
import band.gosrock.admin.service.AdminStartStagingServerUseCase
import band.gosrock.admin.service.AdminStopStagingServerUseCase
import band.gosrock.common.annotation.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal-api/v1")
@SecurityRequirement(name = "admin-token")
@Tag(name = "Admin")
class AdminInfraController(
    private val adminGetStagingServerUseCase: AdminGetStagingServerUseCase,
    private val adminStartStagingServerUseCase: AdminStartStagingServerUseCase,
    private val adminStopStagingServerUseCase: AdminStopStagingServerUseCase,
) {

    @Operation(summary = "스테이징 서버 상태를 조회합니다.")
    @GetMapping("/infra/staging")
    fun getStagingServer(@CurrentUserId userId: Long): AdminStagingServerResponse {
        return adminGetStagingServerUseCase.execute(userId)
    }

    @Operation(summary = "스테이징 서버를 시작합니다. 이미 실행 중이면 현재 상태를 반환합니다.")
    @PostMapping("/infra/staging/start")
    fun startStagingServer(@CurrentUserId userId: Long): AdminStagingServerResponse {
        return adminStartStagingServerUseCase.execute(userId)
    }

    @Operation(summary = "스테이징 서버를 중지합니다. 이미 중지 중이면 현재 상태를 반환합니다.")
    @PostMapping("/infra/staging/stop")
    fun stopStagingServer(@CurrentUserId userId: Long): AdminStagingServerResponse {
        return adminStopStagingServerUseCase.execute(userId)
    }
}
