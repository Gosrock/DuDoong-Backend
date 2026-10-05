package band.gosrock.api.v2.mypage.dto.response

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayRule
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayStatus
import band.gosrock.domain.domains.event.service.v2.V2EventSummaryRow
import band.gosrock.domain.domains.host.service.v2.V2FollowingHostRow
import band.gosrock.domain.domains.host.service.v2.V2MyHostSummaryRow
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.infrastructure.config.s3.ImageUrlDto
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** M-1 내 프로필 (M-2 응답도 같다) */
data class V2MeResponse(
    val userId: Long,
    val name: String?,
    @field:Schema(description = "이메일 (v1 GET /v1/users/me 와 같은 값)")
    val email: String?,
    @field:Schema(description = "프로필 이미지 url. 기본 이미지면 null (카카오 가입 이미지는 카카오 url 그대로)")
    val profileImageUrl: String?,
    @field:Schema(description = "소속(활성) 호스트 — 합류 최신순 최대 10개. 비어 있으면 '아직 두둥에서 호스트가 없어요.'")
    val hosts: List<V2MeHostResponse>,
    @field:Schema(description = "소속(활성) 호스트 전체 수. hosts 보다 많으면 H-1 GET /api/v2/me/hosts 로 전체 목록")
    val hostCount: Long,
) {
    companion object {
        fun of(user: User, hosts: List<V2MyHostSummaryRow>, hostCount: Long): V2MeResponse =
            V2MeResponse(
                userId = user.id!!,
                name = user.profile?.name,
                email = user.profile?.email,
                profileImageUrl = user.profile?.profileImage?.generateImageUrl(),
                hosts = hosts.map { V2MeHostResponse.of(it) },
                hostCount = hostCount,
            )
    }
}

data class V2MeHostResponse(
    val hostId: Long,
    val name: String?,
    val profileImageUrl: String?,
    @field:Schema(description = "내 역할 (MASTER / MANAGER / GUEST)")
    val myRole: String,
) {
    companion object {
        fun of(row: V2MyHostSummaryRow): V2MeHostResponse =
            V2MeHostResponse(
                hostId = row.hostId,
                name = row.name,
                profileImageUrl = ImageVo.valueOf(row.profileImageKey).generateImageUrl(),
                myRole = row.role.name,
            )
    }
}

/** M-3 */
data class V2MeImageUploadResponse(
    @field:Schema(description = "PUT 업로드용 presigned url (3분 유효)")
    val presignedUrl: String,
    @field:Schema(description = "업로드 후 PATCH /api/v2/me 의 profileImageKey 로 보낼 값")
    val key: String,
    @field:Schema(description = "업로드 완료 후 이미지 url")
    val url: String?,
) {
    companion object {
        fun of(urlDto: ImageUrlDto): V2MeImageUploadResponse =
            V2MeImageUploadResponse(
                presignedUrl = urlDto.url,
                key = urlDto.key,
                url = ImageVo.valueOf(urlDto.key).generateImageUrl(),
            )
    }
}

/** M-4 관심 호스트 */
data class V2FollowingHostResponse(
    val hostId: Long,
    val name: String?,
    val profileImageUrl: String?,
    @field:Schema(description = "대표 공연: 진행 중·예정 공개 공연 중 가장 가까운 공연, 없으면 가장 최근에 끝난 공연. 공개 공연이 없으면 null")
    val representativeEvent: V2RepresentativeEventResponse?,
) {
    companion object {
        fun of(row: V2FollowingHostRow, event: V2RepresentativeEventResponse?): V2FollowingHostResponse =
            V2FollowingHostResponse(
                hostId = row.hostId,
                name = row.name,
                profileImageUrl = ImageVo.valueOf(row.profileImageKey).generateImageUrl(),
                representativeEvent = event,
            )
    }
}

data class V2RepresentativeEventResponse(
    val eventId: Long,
    val name: String?,
    val posterImageUrl: String?,
    @field:DateFormat
    val startAt: LocalDateTime?,
    @field:DateFormat
    val endAt: LocalDateTime?,
    @field:Schema(description = "UPCOMING(시작 전) / ONGOING(진행 중) / PAST(공연 종료)")
    val displayStatus: V2EventDisplayStatus,
    // getDDay() 는 Jackson 기본 규칙으로 "dday" 가 되므로 이름을 명시한다
    @get:JsonProperty("dDay")
    @field:Schema(description = "공연일까지 남은 일수 (D-n 의 n, 당일 0). UPCOMING 일 때만, 아니면 null")
    val dDay: Long?,
) {
    companion object {
        fun of(row: V2EventSummaryRow, now: LocalDateTime): V2RepresentativeEventResponse {
            val displayStatus = row.displayStatus(now)
            return V2RepresentativeEventResponse(
                eventId = row.eventId,
                name = row.name,
                posterImageUrl = ImageVo.valueOf(row.posterImageKey).generateImageUrl(),
                startAt = row.startAt,
                endAt = row.endAt,
                displayStatus = displayStatus,
                dDay = V2EventDisplayRule.dDayOf(displayStatus, row.startAt, now),
            )
        }
    }
}

/** M-5 관람 공연 아카이빙 */
data class V2ArchiveResponse(
    @field:Schema(description = "연도 탭: 아카이빙 공연이 있는 공연 시작 연도 (최근 순). year 와 관계없이 전체 기준")
    val years: List<Int>,
    val events: V2PageResponse<V2ArchiveEventResponse>,
)

data class V2ArchiveEventResponse(
    val eventId: Long,
    val name: String?,
    val posterImageUrl: String?,
    @field:DateFormat
    val startAt: LocalDateTime?,
    val host: V2ArchiveHostResponse,
    @field:Schema(description = "주문상세(O-3) 이동용: 이 공연에서 내가 주문하고 입장한 티켓의 주문(가장 최근). 선물받은 티켓으로만 입장했으면 null")
    val orderUuid: String?,
)

data class V2ArchiveHostResponse(
    val hostId: Long,
    val name: String?,
)
