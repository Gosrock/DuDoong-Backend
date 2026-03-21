package band.gosrock.api.admin.controller

import band.gosrock.admin.model.dto.response.AdminUserDetailResponse
import band.gosrock.admin.service.AdminGetMeUseCase
import band.gosrock.common.annotation.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal-api/v1/auth")
@Tag(name = "Admin Auth")
class AdminAuthController(
    private val adminGetMeUseCase: AdminGetMeUseCase,
) {

    @Operation(summary = "현재 어드민 유저 정보 조회")
    @SecurityRequirement(name = "admin-token")
    @GetMapping("/me")
    fun getAdminMe(@CurrentUserId userId: Long): AdminUserDetailResponse {
        return adminGetMeUseCase.execute(userId)
    }
}
