package band.gosrock.api.v2.mypage.controller

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.mypage.dto.request.V2MeImageUploadRequest
import band.gosrock.api.v2.mypage.dto.request.V2UpdateMeRequest
import band.gosrock.api.v2.mypage.dto.response.V2ArchiveResponse
import band.gosrock.api.v2.mypage.dto.response.V2FollowingHostResponse
import band.gosrock.api.v2.mypage.dto.response.V2MeImageUploadResponse
import band.gosrock.api.v2.mypage.dto.response.V2MeResponse
import band.gosrock.api.v2.mypage.usecase.V2MeUseCase
import band.gosrock.api.v2.mypage.usecase.V2ReadArchiveUseCase
import band.gosrock.api.v2.mypage.usecase.V2ReadFollowingHostsUseCase
import band.gosrock.common.annotation.CurrentUserId
import band.gosrock.domain.domains.host.service.v2.V2FollowingHostFilter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(
    name = "v2. 사용자 앱 - 마이페이지",
    description = "M-1~M-5. 알림센터(M-6)는 'v2. 알림센터' N-1~N-3(/api/v2/me/notifications) 재사용, " +
        "주문내역은 O-2·O-3, 소속 호스트 전체는 H-1(/api/v2/me/hosts), 언팔로우는 H-13(DELETE /api/v2/hosts/{hostId}/follow), " +
        "로그아웃·탈퇴는 v1(POST /api/v1/auth/logout, DELETE /api/v1/auth/me) 재사용",
)
@RestController
@RequestMapping("/api/v2/me")
@Validated
class V2MyPageController(
    private val meUseCase: V2MeUseCase,
    private val readFollowingHostsUseCase: V2ReadFollowingHostsUseCase,
    private val readArchiveUseCase: V2ReadArchiveUseCase,
) {
    @Operation(summary = "[M-1] 내 프로필: 닉네임, 이메일, 프로필 이미지, 소속 호스트 요약(합류 최신순 최대 10개)")
    @GetMapping
    fun getMe(@CurrentUserId userId: Long): V2MeResponse = meUseCase.read(userId)

    @Operation(summary = "[M-2] 프로필 수정. null 필드는 변경 안 함. 닉네임은 v1 과 같은 규칙(2~7자), profileImageKey 빈 문자열이면 기본 이미지")
    @PatchMapping
    fun updateMe(
        @CurrentUserId userId: Long,
        @RequestBody @Valid request: V2UpdateMeRequest,
    ): V2MeResponse = meUseCase.update(userId, request)

    @Operation(summary = "[M-3] 프로필 이미지 업로드 url 발급 (JPEG/JPG/PNG). 업로드 후 key 를 M-2 profileImageKey 로")
    @PostMapping("/images")
    fun getImageUploadUrl(
        @CurrentUserId userId: Long,
        @RequestBody @Valid request: V2MeImageUploadRequest,
    ): V2MeImageUploadResponse = meUseCase.getImageUploadUrl(userId, request)

    @Operation(
        summary = "[M-4] 관심 호스트 (최근 팔로우 순) + 호스트별 대표 공연·D-day. " +
            "status: ALL(기본) / ACTIVE(진행 중·예정 공연 있음) / ENDED(끝난 공연만). 언팔로우는 H-13 DELETE /api/v2/hosts/{hostId}/follow",
    )
    @GetMapping("/following-hosts")
    fun getFollowingHosts(
        @CurrentUserId userId: Long,
        @RequestParam(defaultValue = "ALL") status: V2FollowingHostFilter,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) size: Int,
    ): V2PageResponse<V2FollowingHostResponse> = readFollowingHostsUseCase.execute(userId, status, page, size)

    @Operation(
        summary = "[M-5] 관람 공연 아카이빙: 내가(현재 소유자) 입장한 지난 공연, 공연 단위 중복 없음, 최근 공연 순. " +
            "year 는 공연 시작 연도(없으면 전체), years 는 연도 탭 목록",
    )
    @GetMapping("/archive")
    fun getArchive(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) @Min(1) @Max(9999) year: Int?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) size: Int,
    ): V2ArchiveResponse = readArchiveUseCase.execute(userId, year, page, size)

    companion object {
        private const val MAX_PAGE_SIZE = 50L
    }
}
