package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminStagingServerResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.infrastructure.outer.aws.StagingServerClient
import java.time.LocalDateTime

@UseCase
class AdminGetStagingServerUseCase(
    private val adminAuthValidator: AdminAuthValidator,
    private val stagingServerClient: StagingServerClient,
) {

    fun execute(adminUserId: Long): AdminStagingServerResponse {
        adminAuthValidator.validateAdminOrAbove(adminUserId)
        return AdminStagingServerResponse.of(
            stagingServerClient.describe(),
            LocalDateTime.now(AdminStagingServerResponse.KST),
        )
    }
}
