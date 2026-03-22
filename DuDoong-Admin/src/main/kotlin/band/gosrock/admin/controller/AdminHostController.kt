package band.gosrock.admin.controller

import band.gosrock.admin.model.dto.request.AdminAddHostMemberRequest
import band.gosrock.admin.model.dto.request.AdminTransferMasterRequest
import band.gosrock.admin.model.dto.request.AdminUpdateHostMemberRoleRequest
import band.gosrock.admin.model.dto.request.AdminUpdateHostPartnerRequest
import band.gosrock.admin.model.dto.request.AdminUpdateHostProfileRequest
import band.gosrock.admin.model.dto.response.AdminEventResponse
import band.gosrock.admin.model.dto.response.AdminHostDetailResponse
import band.gosrock.admin.model.dto.response.AdminHostMemberResponse
import band.gosrock.admin.model.dto.response.AdminHostResponse
import band.gosrock.admin.service.AdminAddHostMemberUseCase
import band.gosrock.admin.service.AdminTransferMasterUseCase
import band.gosrock.admin.service.AdminGetHostDetailUseCase
import band.gosrock.admin.service.AdminGetHostEventsUseCase
import band.gosrock.admin.service.AdminGetHostMembersUseCase
import band.gosrock.admin.service.AdminGetHostsUseCase
import band.gosrock.admin.service.AdminRemoveHostMemberUseCase
import band.gosrock.admin.service.AdminUpdateHostMemberRoleUseCase
import band.gosrock.admin.service.AdminUpdateHostPartnerUseCase
import band.gosrock.admin.service.AdminUpdateHostProfileUseCase
import band.gosrock.common.annotation.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal-api/v1/hosts")
@SecurityRequirement(name = "admin-token")
@Tag(name = "Admin")
class AdminHostController(
    private val adminGetHostsUseCase: AdminGetHostsUseCase,
    private val adminGetHostDetailUseCase: AdminGetHostDetailUseCase,
    private val adminGetHostMembersUseCase: AdminGetHostMembersUseCase,
    private val adminUpdateHostMemberRoleUseCase: AdminUpdateHostMemberRoleUseCase,
    private val adminAddHostMemberUseCase: AdminAddHostMemberUseCase,
    private val adminRemoveHostMemberUseCase: AdminRemoveHostMemberUseCase,
    private val adminGetHostEventsUseCase: AdminGetHostEventsUseCase,
    private val adminUpdateHostPartnerUseCase: AdminUpdateHostPartnerUseCase,
    private val adminUpdateHostProfileUseCase: AdminUpdateHostProfileUseCase,
    private val adminTransferMasterUseCase: AdminTransferMasterUseCase,
) {

    @Operation(summary = "호스트 목록을 조회합니다.")
    @GetMapping
    fun getHosts(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) keyword: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<AdminHostResponse> {
        return adminGetHostsUseCase.execute(userId, keyword, pageable)
    }

    @Operation(summary = "호스트 상세 정보를 조회합니다.")
    @GetMapping("/{hostId}")
    fun getHostDetail(@CurrentUserId userId: Long, @PathVariable hostId: Long): AdminHostDetailResponse {
        return adminGetHostDetailUseCase.execute(userId, hostId)
    }

    @Operation(summary = "호스트 소속 멤버 목록을 조회합니다.")
    @GetMapping("/{hostId}/members")
    fun getHostMembers(@CurrentUserId userId: Long, @PathVariable hostId: Long): List<AdminHostMemberResponse> {
        return adminGetHostMembersUseCase.execute(userId, hostId)
    }

    @Operation(summary = "호스트 멤버 역할을 변경합니다.")
    @PatchMapping("/{hostId}/members/{targetUserId}/role")
    fun updateHostMemberRole(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @PathVariable targetUserId: Long,
        @RequestBody request: AdminUpdateHostMemberRoleRequest,
    ): AdminHostMemberResponse {
        return adminUpdateHostMemberRoleUseCase.execute(userId, hostId, targetUserId, request)
    }

    @Operation(summary = "호스트에 멤버를 추가합니다.")
    @PostMapping("/{hostId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    fun addHostMember(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestBody request: AdminAddHostMemberRequest,
    ): AdminHostMemberResponse {
        return adminAddHostMemberUseCase.execute(userId, hostId, request)
    }

    @Operation(summary = "호스트에서 멤버를 제거합니다.")
    @DeleteMapping("/{hostId}/members/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeHostMember(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @PathVariable targetUserId: Long,
    ) {
        adminRemoveHostMemberUseCase.execute(userId, hostId, targetUserId)
    }

    @Operation(summary = "호스트별 이벤트 목록을 조회합니다.")
    @GetMapping("/{hostId}/events")
    fun getHostEvents(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<AdminEventResponse> {
        return adminGetHostEventsUseCase.execute(userId, hostId, pageable)
    }

    @Operation(summary = "호스트의 파트너 여부를 변경합니다.")
    @PatchMapping("/{hostId}/partner")
    fun updateHostPartner(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestBody request: AdminUpdateHostPartnerRequest,
    ): AdminHostDetailResponse {
        return adminUpdateHostPartnerUseCase.execute(userId, hostId, request)
    }

    @Operation(summary = "호스트 프로필을 수정합니다.")
    @PatchMapping("/{hostId}/profile")
    fun updateHostProfile(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestBody request: AdminUpdateHostProfileRequest,
    ): AdminHostDetailResponse {
        return adminUpdateHostProfileUseCase.execute(userId, hostId, request)
    }

    @Operation(summary = "호스트 마스터 권한을 강제 양도합니다. (어드민)")
    @PostMapping("/{hostId}/transfer-master")
    fun transferMaster(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestBody request: AdminTransferMasterRequest,
    ): AdminHostDetailResponse {
        return adminTransferMasterUseCase.execute(userId, hostId, request)
    }
}
