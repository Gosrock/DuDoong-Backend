package band.gosrock.api.host.controller

import band.gosrock.api.common.page.PageResponse
import band.gosrock.api.common.slice.SliceResponse
import band.gosrock.api.host.model.dto.request.CreateHostRequest
import band.gosrock.api.host.model.dto.request.InviteHostRequest
import band.gosrock.api.host.model.dto.request.UpdateHostRequest
import band.gosrock.api.host.model.dto.request.UpdateHostSlackRequest
import band.gosrock.api.host.model.dto.request.TransferMasterRequest
import band.gosrock.api.host.model.dto.request.UpdateHostUserRoleRequest
import band.gosrock.api.host.model.dto.response.HostDetailResponse
import band.gosrock.api.host.model.dto.response.HostEventProfileResponse
import band.gosrock.api.host.model.dto.response.HostProfileResponse
import band.gosrock.api.host.model.dto.response.HostResponse
import band.gosrock.api.host.service.CreateHostUseCase
import band.gosrock.api.host.service.InviteHostUseCase
import band.gosrock.api.host.service.JoinHostUseCase
import band.gosrock.api.host.service.ReadHostEventsUseCase
import band.gosrock.api.host.service.ReadHostProfilesUseCase
import band.gosrock.api.host.service.ReadHostUseCase
import band.gosrock.api.host.service.TransferMasterUseCase
import band.gosrock.api.host.service.ReadInviteUsersUseCase
import band.gosrock.api.host.service.RejectHostUseCase
import band.gosrock.api.host.service.UpdateHostProfileUseCase
import band.gosrock.api.host.service.UpdateHostSlackUrlUseCase
import band.gosrock.api.host.service.UpdateHostUserRoleUseCase
import band.gosrock.common.annotation.CurrentUserId
import band.gosrock.domain.common.vo.UserProfileVo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = "4. [호스트]")
@RestController
@RequestMapping("/api/v1/hosts")
@Validated
class HostController(
    private val readHostUseCase: ReadHostUseCase,
    private val readHostsUseCase: ReadHostProfilesUseCase,
    private val readHostEventsUseCase: ReadHostEventsUseCase,
    private val readInviteUsersUseCase: ReadInviteUsersUseCase,
    private val createHostUseCase: CreateHostUseCase,
    private val updateHostProfileUseCase: UpdateHostProfileUseCase,
    private val updateHostSlackUrlUseCase: UpdateHostSlackUrlUseCase,
    private val updateHostUserRoleUseCase: UpdateHostUserRoleUseCase,
    private val inviteHostUseCase: InviteHostUseCase,
    private val joinHostUseCase: JoinHostUseCase,
    private val rejectHostUseCase: RejectHostUseCase,
    private val transferMasterUseCase: TransferMasterUseCase,
) {
    @Operation(summary = "내가 속한 호스트 리스트를 가져옵니다.")
    @GetMapping
    fun getAllHosts(
        @CurrentUserId userId: Long,
        @ParameterObject @PageableDefault(size = 10) pageable: Pageable
    ): SliceResponse<HostProfileResponse> {
        return readHostsUseCase.execute(userId, pageable)
    }

    @Operation(summary = "고유 아이디에 해당하는 호스트 정보를 가져옵니다.")
    @GetMapping("/{hostId}")
    fun getHostById(@PathVariable hostId: Long): HostDetailResponse {
        return readHostUseCase.execute(hostId)
    }

    @Operation(summary = "해당 호스트에 가입하지 않은 유저를 이메일로 검색합니다.")
    @GetMapping("/{hostId}/invite/users")
    fun getInviteUserListByEmail(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestParam(value = "email") @Email email: String,
    ): UserProfileVo {
        return readInviteUsersUseCase.execute(userId, hostId, email)
    }

    @Operation(summary = "해당 호스트가 관리중인 이벤트 리스트를 가져옵니다.")
    @GetMapping("/{hostId}/events")
    fun getHostEventsById(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @ParameterObject @PageableDefault(size = 10) pageable: Pageable,
    ): PageResponse<HostEventProfileResponse> {
        return readHostEventsUseCase.execute(userId, hostId, pageable)
    }

    @Operation(summary = "호스트 간편 생성. 호스트를 생성한 유저 자신은 마스터 호스트가 됩니다.")
    @PostMapping
    fun createHost(@CurrentUserId userId: Long, @RequestBody @Valid createEventRequest: CreateHostRequest): HostResponse {
        return createHostUseCase.execute(userId, createEventRequest)
    }

    @Operation(summary = "초대 받은 호스트에 가입합니다.")
    @PostMapping("/{hostId}/join")
    fun joinHost(@CurrentUserId userId: Long, @PathVariable hostId: Long): HostDetailResponse {
        return joinHostUseCase.execute(userId, hostId)
    }

    @Operation(summary = "호스트 초대를 거절합니다.")
    @PostMapping("/{hostId}/reject")
    fun rejectHost(@CurrentUserId userId: Long, @PathVariable hostId: Long): HostDetailResponse {
        return rejectHostUseCase.execute(userId, hostId)
    }

    @Operation(summary = "다른 유저를 호스트 유저로 초대합니다.")
    @PostMapping("/{hostId}/invite")
    fun inviteHost(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestBody @Valid inviteHostRequest: InviteHostRequest,
    ): HostDetailResponse {
        return inviteHostUseCase.execute(userId, hostId, inviteHostRequest)
    }

    @Operation(summary = "호스트 유저의 권한을 변경합니다. 매니저 이상만 가능합니다.")
    @PatchMapping("/{hostId}/role")
    fun patchHostUserRole(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestBody @Valid updateHostUserRoleRequest: UpdateHostUserRoleRequest,
    ): HostDetailResponse {
        return updateHostUserRoleUseCase.execute(userId, hostId, updateHostUserRoleRequest)
    }

    @Operation(summary = "호스트 정보를 변경합니다. 매니저 이상만 가능합니다.")
    @PatchMapping("/{hostId}/profile")
    fun patchHostById(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestBody @Valid updateHostRequest: UpdateHostRequest,
    ): HostDetailResponse {
        return updateHostProfileUseCase.execute(userId, hostId, updateHostRequest)
    }

    @Operation(summary = "호스트 슬랙 알람 URL 을 변경합니다. 매니저 이상만 가능합니다.")
    @PatchMapping("/{hostId}/slack")
    fun patchHostSlackUrlById(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestBody @Valid updateHostSlackRequest: UpdateHostSlackRequest,
    ): HostDetailResponse {
        return updateHostSlackUrlUseCase.execute(userId, hostId, updateHostSlackRequest)
    }

    @Operation(summary = "호스트 마스터 권한을 다른 멤버에게 양도합니다. 마스터만 가능합니다.")
    @PostMapping("/{hostId}/transfer-master")
    fun transferMaster(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestBody @Valid request: TransferMasterRequest,
    ): HostDetailResponse {
        return transferMasterUseCase.execute(userId, hostId, request)
    }
}
