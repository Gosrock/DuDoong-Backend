package band.gosrock.api.admin.controller

import band.gosrock.api.admin.model.dto.request.AdminUpdateUserRoleRequest
import band.gosrock.api.admin.model.dto.request.AdminUpdateUserStatusRequest
import band.gosrock.api.admin.model.dto.response.AdminUserDetailResponse
import band.gosrock.api.admin.model.dto.response.AdminUserResponse
import band.gosrock.api.admin.service.AdminGetUserDetailUseCase
import band.gosrock.api.admin.service.AdminGetUsersUseCase
import band.gosrock.api.admin.service.AdminUpdateUserRoleUseCase
import band.gosrock.api.admin.service.AdminUpdateUserStatusUseCase
import band.gosrock.api.config.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal-api/v1/users")
@SecurityRequirement(name = "access-token")
@Tag(name = "Admin")
class AdminUserController(
    private val adminGetUsersUseCase: AdminGetUsersUseCase,
    private val adminGetUserDetailUseCase: AdminGetUserDetailUseCase,
    private val adminUpdateUserRoleUseCase: AdminUpdateUserRoleUseCase,
    private val adminUpdateUserStatusUseCase: AdminUpdateUserStatusUseCase,
) {

    @Operation(summary = "유저 목록을 조회합니다.")
    @GetMapping
    fun getUsers(
        @RequestParam(required = false) keyword: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<AdminUserResponse> {
        return adminGetUsersUseCase.execute(keyword, pageable)
    }

    @Operation(summary = "유저 상세 정보를 조회합니다.")
    @GetMapping("/{userId}")
    fun getUserDetail(@PathVariable userId: Long): AdminUserDetailResponse {
        return adminGetUserDetailUseCase.execute(userId)
    }

    @Operation(summary = "유저 역할을 변경합니다. (SUPER_ADMIN 전용)")
    @PatchMapping("/{userId}/role")
    fun updateUserRole(
        @CurrentUserId currentUserId: Long,
        @PathVariable userId: Long,
        @RequestBody request: AdminUpdateUserRoleRequest,
    ): AdminUserResponse {
        return adminUpdateUserRoleUseCase.execute(currentUserId, userId, request)
    }

    @Operation(summary = "유저 상태를 변경합니다.")
    @PatchMapping("/{userId}/status")
    fun updateUserStatus(
        @PathVariable userId: Long,
        @RequestBody request: AdminUpdateUserStatusRequest,
    ): AdminUserResponse {
        return adminUpdateUserStatusUseCase.execute(userId, request)
    }
}
