package band.gosrock.admin.service

import band.gosrock.admin.exception.StagingServerNotConfiguredException
import band.gosrock.admin.model.dto.response.AdminStagingServerResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.infrastructure.outer.aws.StagingServerClient
import band.gosrock.infrastructure.outer.aws.StagingServerState
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

@UseCase
class AdminStopStagingServerUseCase(
    private val adminAuthValidator: AdminAuthValidator,
    private val stagingServerClient: StagingServerClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(adminUserId: Long): AdminStagingServerResponse {
        adminAuthValidator.validateAdminOrAbove(adminUserId)
        if (!stagingServerClient.isConfigured()) throw StagingServerNotConfiguredException.EXCEPTION

        val current = stagingServerClient.describe()
        if (current.state == StagingServerState.STOPPED || current.state == StagingServerState.STOPPING) {
            log.info("[ADMIN-INFRA] STAGING STOP SKIPPED - userId={}, state={}", adminUserId, current.state)
            return AdminStagingServerResponse.of(current, LocalDateTime.now(AdminStagingServerResponse.KST))
        }

        log.info("[ADMIN-INFRA] STAGING STOP - userId={}, from={}", adminUserId, current.state)
        stagingServerClient.stop()
        return AdminStagingServerResponse.of(
            stagingServerClient.describe(),
            LocalDateTime.now(AdminStagingServerResponse.KST),
        )
    }
}
