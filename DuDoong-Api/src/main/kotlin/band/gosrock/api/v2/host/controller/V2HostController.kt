package band.gosrock.api.v2.host.controller

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.host.dto.request.V2AddHostMembersRequest
import band.gosrock.api.v2.host.dto.request.V2CreateHostRequest
import band.gosrock.api.v2.host.dto.request.V2HostImageUploadRequest
import band.gosrock.api.v2.host.dto.request.V2TransferMasterRequest
import band.gosrock.api.v2.host.dto.request.V2UpdateHostMemberRoleRequest
import band.gosrock.api.v2.host.dto.request.V2UpdateHostRequest
import band.gosrock.api.v2.host.dto.response.V2CreateHostResponse
import band.gosrock.api.v2.host.dto.response.V2HostEventResponse
import band.gosrock.api.v2.host.dto.response.V2HostFollowResponse
import band.gosrock.api.v2.host.dto.response.V2HostHomeResponse
import band.gosrock.api.v2.host.dto.response.V2HostImageUploadResponse
import band.gosrock.api.v2.host.dto.response.V2HostMemberResponse
import band.gosrock.api.v2.host.dto.response.V2MyHostResponse
import band.gosrock.api.v2.host.usecase.V2AddHostMembersUseCase
import band.gosrock.api.v2.host.usecase.V2CreateHostUseCase
import band.gosrock.api.v2.host.usecase.V2GetHostImageUploadUrlUseCase
import band.gosrock.api.v2.host.usecase.V2HostFollowUseCase
import band.gosrock.api.v2.host.usecase.V2ReadHostEventsUseCase
import band.gosrock.api.v2.host.usecase.V2ReadHostHomeUseCase
import band.gosrock.api.v2.host.usecase.V2ReadHostMembersUseCase
import band.gosrock.api.v2.host.usecase.V2ReadMyHostsUseCase
import band.gosrock.api.v2.host.usecase.V2RemoveHostMemberUseCase
import band.gosrock.api.v2.host.usecase.V2TransferMasterUseCase
import band.gosrock.api.v2.host.usecase.V2UpdateHostMemberRoleUseCase
import band.gosrock.api.v2.host.usecase.V2UpdateHostUseCase
import band.gosrock.common.annotation.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = "v2. 호스트 / 멤버")
@RestController
@RequestMapping("/api/v2")
@Validated
class V2HostController(
    private val readMyHostsUseCase: V2ReadMyHostsUseCase,
    private val createHostUseCase: V2CreateHostUseCase,
    private val readHostHomeUseCase: V2ReadHostHomeUseCase,
    private val updateHostUseCase: V2UpdateHostUseCase,
    private val readHostMembersUseCase: V2ReadHostMembersUseCase,
    private val addHostMembersUseCase: V2AddHostMembersUseCase,
    private val updateHostMemberRoleUseCase: V2UpdateHostMemberRoleUseCase,
    private val removeHostMemberUseCase: V2RemoveHostMemberUseCase,
    private val transferMasterUseCase: V2TransferMasterUseCase,
    private val hostFollowUseCase: V2HostFollowUseCase,
    private val readHostEventsUseCase: V2ReadHostEventsUseCase,
    private val getHostImageUploadUrlUseCase: V2GetHostImageUploadUrlUseCase,
) {
    @Operation(summary = "[H-1] 내가 속한(활성) 호스트 목록. keyword 는 이름 부분일치")
    @GetMapping("/me/hosts")
    fun getMyHosts(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) size: Int,
    ): V2PageResponse<V2MyHostResponse> = readMyHostsUseCase.execute(userId, keyword, page, size)

    @Operation(summary = "[H-2] 호스트 생성. 생성자는 마스터")
    @PostMapping("/hosts")
    fun createHost(
        @CurrentUserId userId: Long,
        @RequestBody @Valid request: V2CreateHostRequest,
    ): V2CreateHostResponse = createHostUseCase.execute(userId, request)

    @Operation(summary = "[H-3] 공개 호스트 홈 (비로그인 허용)")
    @GetMapping("/hosts/{hostId}")
    fun getHostHome(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
    ): V2HostHomeResponse = readHostHomeUseCase.execute(userId, hostId)

    @Operation(summary = "[H-4] 호스트 설정 수정 (매니저 이상). null 필드는 변경 안 함, contacts 는 전체 교체")
    @PatchMapping("/hosts/{hostId}")
    fun updateHost(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestBody @Valid request: V2UpdateHostRequest,
    ): V2HostHomeResponse = updateHostUseCase.execute(userId, hostId, request)

    @Operation(summary = "[H-6] 멤버 목록 (일반 이상, 활성 멤버만)")
    @GetMapping("/hosts/{hostId}/members")
    fun getMembers(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
    ): List<V2HostMemberResponse> = readHostMembersUseCase.execute(userId, hostId)

    @Operation(summary = "[H-7] 멤버 일괄 추가 (매니저 이상, 수락 없이 즉시 추가). 매니저는 GUEST 만")
    @PostMapping("/hosts/{hostId}/members")
    fun addMembers(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestBody @Valid request: V2AddHostMembersRequest,
    ): List<V2HostMemberResponse> = addHostMembersUseCase.execute(userId, hostId, request)

    @Operation(summary = "[H-10] 멤버 역할 변경 GUEST ↔ MANAGER (마스터만)")
    @PatchMapping("/hosts/{hostId}/members/{userId}/role")
    fun updateMemberRole(
        @CurrentUserId currentUserId: Long,
        @PathVariable hostId: Long,
        @PathVariable("userId") targetUserId: Long,
        @RequestBody @Valid request: V2UpdateHostMemberRoleRequest,
    ): List<V2HostMemberResponse> = updateHostMemberRoleUseCase.execute(currentUserId, hostId, targetUserId, request)

    @Operation(summary = "[H-11] 멤버 삭제 (매니저 이상). 마스터 삭제 불가, 매니저는 GUEST 만")
    @DeleteMapping("/hosts/{hostId}/members/{userId}")
    fun removeMember(
        @CurrentUserId currentUserId: Long,
        @PathVariable hostId: Long,
        @PathVariable("userId") targetUserId: Long,
    ): List<V2HostMemberResponse> = removeHostMemberUseCase.execute(currentUserId, hostId, targetUserId)

    @Operation(summary = "[H-12] 마스터 양도 (마스터만). 기존 마스터는 매니저가 된다")
    @PostMapping("/hosts/{hostId}/master-transfer")
    fun transferMaster(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestBody @Valid request: V2TransferMasterRequest,
    ): List<V2HostMemberResponse> = transferMasterUseCase.execute(userId, hostId, request)

    @Operation(summary = "[H-13] 호스트 팔로우 (멱등)")
    @PutMapping("/hosts/{hostId}/follow")
    fun follow(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
    ): V2HostFollowResponse = hostFollowUseCase.follow(userId, hostId)

    @Operation(summary = "[H-13] 호스트 팔로우 해제 (멱등)")
    @DeleteMapping("/hosts/{hostId}/follow")
    fun unfollow(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
    ): V2HostFollowResponse = hostFollowUseCase.unfollow(userId, hostId)

    @Operation(summary = "[H-14] 호스트 공연 리스트 (비로그인 허용). 비멤버는 공개 공연만, 멤버는 준비중 포함")
    @GetMapping("/hosts/{hostId}/events")
    fun getHostEvents(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) size: Int,
    ): V2PageResponse<V2HostEventResponse> = readHostEventsUseCase.execute(userId, hostId, page, size)

    @Operation(summary = "[H-15] 호스트 프로필/커버 이미지 업로드 url 발급 (매니저 이상)")
    @PostMapping("/hosts/{hostId}/images")
    fun getImageUploadUrl(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestBody @Valid request: V2HostImageUploadRequest,
    ): V2HostImageUploadResponse = getHostImageUploadUrlUseCase.execute(userId, hostId, request)

    companion object {
        private const val MAX_PAGE_SIZE = 50L
    }
}
