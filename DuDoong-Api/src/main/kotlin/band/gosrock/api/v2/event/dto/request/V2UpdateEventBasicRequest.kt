package band.gosrock.api.v2.event.dto.request

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.event.domain.EventContact
import band.gosrock.domain.domains.event.domain.EventPlace
import band.gosrock.domain.domains.event.service.v2.V2EventDomainService
import band.gosrock.domain.domains.host.domain.HostContactType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/** 기본 정보 부분 수정. null 인 필드는 변경하지 않는다. 등록(OPEN) 후에도 수정 가능, hasTicket 은 준비중일 때만 */
data class V2UpdateEventBasicRequest(
    @field:Schema(description = "포스터 이미지 key (이미지 업로드 API 응답의 key). null 이면 변경 안 함, 빈 문자열이면 포스터 제거")
    @field:Size(max = 255)
    val posterImageKey: String? = null,

    @field:Schema(description = "공연 이름 (1~25자)")
    @field:Size(min = 1, max = 25)
    @field:Pattern(regexp = ".*\\S.*", message = "공백만으로는 이름을 지을 수 없습니다")
    val name: String? = null,

    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "공연 시작 시각")
    @field:DateFormat
    val startAt: LocalDateTime? = null,

    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "공연 종료 시각 (시작 이후)")
    @field:DateFormat
    val endAt: LocalDateTime? = null,

    @field:Schema(description = "공연 장소 (상세주소 외 모든 필드 필수). 통째로 교체")
    @field:Valid
    val place: V2EventPlaceRequest? = null,

    @field:Schema(description = "문의처 전체 교체 (0~${V2EventDomainService.MAX_CONTACT_COUNT}개)")
    @field:Size(max = V2EventDomainService.MAX_CONTACT_COUNT)
    @field:Valid
    val contacts: List<V2EventContactRequest?>? = null,

    @field:Schema(description = "티켓 여부. 준비중일 때만 변경 가능 (같은 값은 허용)")
    val hasTicket: Boolean? = null,

    @field:Schema(description = "태그 id 전체 교체 (중복 제거 후 최대 ${V2EventDomainService.MAX_TAG_COUNT}개)")
    @field:Size(max = TAG_IDS_RAW_MAX)
    val tagIds: List<Long?>? = null,
) {
    companion object {
        // 개수 검증은 중복 제거 후 도메인에서 한다. 여기서는 과도한 요청만 막는다
        private const val TAG_IDS_RAW_MAX = 100
    }
}

data class V2EventPlaceRequest(
    @field:Schema(description = "공연장 이름", example = "롤링홀")
    @field:NotBlank
    @field:Size(max = 255)
    val name: String?,

    @field:Schema(description = "공연장 주소", example = "서울 마포구 어울마당로 35")
    @field:NotBlank
    @field:Size(max = 255)
    val address: String?,

    @field:Schema(description = "위도", example = "37.548369")
    @field:NotNull
    val latitude: Double?,

    @field:Schema(description = "경도", example = "126.920036")
    @field:NotNull
    val longitude: Double?,

    @field:Schema(description = "상세주소 (선택, 최대 255자). 장소는 통째로 교체되므로 비우거나 빼면 상세주소가 지워진다", example = "지하 1층")
    @field:Size(max = 255)
    val detailAddress: String? = null,
) {
    fun toEventPlace(): EventPlace =
        EventPlace(
            latitude = latitude,
            longitude = longitude,
            placeName = name!!.trim(),
            placeAddress = address!!.trim(),
            placeDetailAddress = sanitizeDetailAddress(detailAddress),
        )

    companion object {
        /** 한 줄 텍스트로 정리: 제어 문자(줄바꿈·탭 포함) 제거 → 앞뒤 공백 제거 → 비면 null */
        fun sanitizeDetailAddress(value: String?): String? =
            value?.filterNot { it.isISOControl() }?.trim()?.ifEmpty { null }
    }
}

data class V2EventContactRequest(
    @field:Schema(description = "문의처 유형", example = "INSTAGRAM")
    @field:NotNull
    val type: HostContactType?,

    @field:Schema(description = "문의처 값", example = "@gosrock")
    @field:NotBlank
    @field:Size(max = EventContact.VALUE_MAX_LENGTH)
    val value: String?,
) {
    fun toEntity(): EventContact = EventContact(type = type!!, value = value!!.trim())
}
