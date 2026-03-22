package band.gosrock.admin.controller

import band.gosrock.admin.model.dto.request.AdminChangeNameRequest
import band.gosrock.admin.model.dto.request.AdminUpdateUserRoleRequest
import band.gosrock.admin.model.dto.request.AdminUpdateUserStatusRequest
import band.gosrock.admin.model.dto.response.AdminUserDetailResponse
import band.gosrock.admin.model.dto.response.AdminUserResponse
import band.gosrock.admin.service.AdminChangeNameUseCase
import band.gosrock.admin.service.AdminExcelService
import band.gosrock.admin.service.AdminGetUserDetailUseCase
import band.gosrock.admin.service.AdminGetUsersUseCase
import band.gosrock.admin.service.AdminUpdateUserRoleUseCase
import band.gosrock.admin.service.AdminUpdateUserStatusUseCase
import band.gosrock.common.annotation.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal-api/v1/users")
@SecurityRequirement(name = "admin-token")
@Tag(name = "Admin")
class AdminUserController(
    private val adminGetUsersUseCase: AdminGetUsersUseCase,
    private val adminGetUserDetailUseCase: AdminGetUserDetailUseCase,
    private val adminUpdateUserRoleUseCase: AdminUpdateUserRoleUseCase,
    private val adminUpdateUserStatusUseCase: AdminUpdateUserStatusUseCase,
    private val adminChangeNameUseCase: AdminChangeNameUseCase,
    private val adminExcelService: AdminExcelService,
) {

    @Operation(summary = "유저 목록을 엑셀로 다운로드합니다.")
    @GetMapping("/export")
    fun exportUsers(
        @CurrentUserId currentUserId: Long,
        @RequestParam(required = false) keyword: String?,
    ): ResponseEntity<ByteArray> {
        val users = adminGetUsersUseCase.executeAll(currentUserId, keyword)
        val bytes = adminExcelService.generateUsersExcel(users)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(bytes)
    }

    @Operation(summary = "유저 목록을 조회합니다.")
    @GetMapping
    fun getUsers(
        @CurrentUserId currentUserId: Long,
        @RequestParam(required = false) keyword: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<AdminUserResponse> {
        return adminGetUsersUseCase.execute(currentUserId, keyword, pageable)
    }

    @Operation(summary = "유저 상세 정보를 조회합니다.")
    @GetMapping("/{userId}")
    fun getUserDetail(
        @CurrentUserId currentUserId: Long,
        @PathVariable userId: Long,
    ): AdminUserDetailResponse {
        return adminGetUserDetailUseCase.execute(currentUserId, userId)
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
        @CurrentUserId currentUserId: Long,
        @PathVariable userId: Long,
        @RequestBody request: AdminUpdateUserStatusRequest,
    ): AdminUserResponse {
        return adminUpdateUserStatusUseCase.execute(currentUserId, userId, request)
    }

    @Operation(summary = "유저 이름 변경")
    @PatchMapping("/{userId}/name")
    fun changeUserName(
        @CurrentUserId adminUserId: Long,
        @PathVariable userId: Long,
        @Valid @RequestBody request: AdminChangeNameRequest,
    ) {
        adminChangeNameUseCase.execute(adminUserId, userId, request)
    }
}
