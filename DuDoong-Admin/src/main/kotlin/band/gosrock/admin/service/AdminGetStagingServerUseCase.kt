package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminStagingServerResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.infrastructure.outer.aws.StagingAppHealthChecker
import band.gosrock.infrastructure.outer.aws.StagingServerClient
import band.gosrock.infrastructure.outer.aws.StagingServerState
import java.time.LocalDateTime

@UseCase
class AdminGetStagingServerUseCase(
    private val adminAuthValidator: AdminAuthValidator,
    private val stagingServerClient: StagingServerClient,
    private val stagingAppHealthChecker: StagingAppHealthChecker,
) {

    fun execute(adminUserId: Long): AdminStagingServerResponse {
        adminAuthValidator.validateAdminOrAbove(adminUserId)
        val now = LocalDateTime.now(AdminStagingServerResponse.KST)
        val info = stagingServerClient.describe()
        val base = AdminStagingServerResponse.of(info, now)
        if (info.state != StagingServerState.RUNNING) return base

        // 서버가 켜져 있으면 앱이 실제로 떴는지 사설 IP로 확인한다
        val healthy = info.privateIp?.let { stagingAppHealthChecker.isHealthy(it) } ?: false
        return base.copy(appStatus = AdminStagingServerResponse.appStatusOf(healthy, base.launchedAt, now))
    }
}
