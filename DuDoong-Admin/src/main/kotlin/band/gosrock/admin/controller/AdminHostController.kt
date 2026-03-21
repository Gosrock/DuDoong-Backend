package band.gosrock.admin.controller

import band.gosrock.admin.model.dto.request.AdminAddHostMemberRequest
import band.gosrock.admin.model.dto.request.AdminUpdateHostMemberRoleRequest
import band.gosrock.admin.model.dto.request.AdminUpdateHostPartnerRequest
import band.gosrock.admin.model.dto.request.AdminUpdateHostProfileRequest
import band.gosrock.admin.model.dto.response.AdminEventResponse
import band.gosrock.admin.model.dto.response.AdminHostDetailResponse
import band.gosrock.admin.model.dto.response.AdminHostMemberResponse
import band.gosrock.admin.model.dto.response.AdminHostResponse
import band.gosrock.admin.service.AdminAddHostMemberUseCase
import band.gosrock.admin.service.AdminGetHostDetailUseCase
import band.gosrock.admin.service.AdminGetHostEventsUseCase
import band.gosrock.admin.service.AdminGetHostMembersUseCase
import band.gosrock.admin.service.AdminGetHostsUseCase
import band.gosrock.admin.service.AdminRemoveHostMemberUseCase
import band.gosrock.admin.service.AdminUpdateHostMemberRoleUseCase
import band.gosrock.admin.service.AdminUpdateHostPartnerUseCase
import band.gosrock.admin.service.AdminUpdateHostProfileUseCase
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
) {

    @Operation(summary = "호스트 목록을 조회합니다.")
    @GetMapping
    fun getHosts(
        @RequestParam(required = false) keyword: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<AdminHostResponse> {
        return adminGetHostsUseCase.execute(keyword, pageable)
    }

    @Operation(summary = "호스트 상세 정보를 조회합니다.")
    @GetMapping("/{hostId}")
    fun getHostDetail(@PathVariable hostId: Long): AdminHostDetailResponse {
        return adminGetHostDetailUseCase.execute(hostId)
    }

    @Operation(summary = "호스트 소속 멤버 목록을 조회합니다.")
    @GetMapping("/{hostId}/members")
    fun getHostMembers(@PathVariable hostId: Long): List<AdminHostMemberResponse> {
        return adminGetHostMembersUseCase.execute(hostId)
    }

    @Operation(summary = "호스트 멤버 역할을 변경합니다.")
    @PatchMapping("/{hostId}/members/{userId}/role")
    fun updateHostMemberRole(
        @PathVariable hostId: Long,
        @PathVariable userId: Long,
        @RequestBody request: AdminUpdateHostMemberRoleRequest,
    ): AdminHostMemberResponse {
        return adminUpdateHostMemberRoleUseCase.execute(hostId, userId, request)
    }

    @Operation(summary = "호스트에 멤버를 추가합니다.")
    @PostMapping("/{hostId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    fun addHostMember(
        @PathVariable hostId: Long,
        @RequestBody request: AdminAddHostMemberRequest,
    ): AdminHostMemberResponse {
        return adminAddHostMemberUseCase.execute(hostId, request)
    }

    @Operation(summary = "호스트에서 멤버를 제거합니다.")
    @DeleteMapping("/{hostId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeHostMember(
        @PathVariable hostId: Long,
        @PathVariable userId: Long,
    ) {
        adminRemoveHostMemberUseCase.execute(hostId, userId)
    }

    @Operation(summary = "호스트별 이벤트 목록을 조회합니다.")
    @GetMapping("/{hostId}/events")
    fun getHostEvents(
        @PathVariable hostId: Long,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<AdminEventResponse> {
        return adminGetHostEventsUseCase.execute(hostId, pageable)
    }

    @Operation(summary = "호스트의 파트너 여부를 변경합니다.")
    @PatchMapping("/{hostId}/partner")
    fun updateHostPartner(
        @PathVariable hostId: Long,
        @RequestBody request: AdminUpdateHostPartnerRequest,
    ): AdminHostDetailResponse {
        return adminUpdateHostPartnerUseCase.execute(hostId, request)
    }

    @Operation(summary = "호스트 프로필을 수정합니다.")
    @PatchMapping("/{hostId}/profile")
    fun updateHostProfile(
        @PathVariable hostId: Long,
        @RequestBody request: AdminUpdateHostProfileRequest,
    ): AdminHostDetailResponse {
        return adminUpdateHostProfileUseCase.execute(hostId, request)
    }
}
