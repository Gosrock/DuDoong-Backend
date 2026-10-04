package band.gosrock.api.v2.host.dto.request

import band.gosrock.domain.domains.host.domain.Host
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** 호스트 설정 부분 수정. null 인 필드는 변경하지 않는다 */
data class V2UpdateHostRequest(
    @field:Schema(description = "호스트 이름 (1~15자). null 이면 변경 안 함")
    @field:Size(min = 1, max = 15)
    @field:Pattern(regexp = ".*\\S.*", message = "공백만으로는 이름을 지을 수 없습니다")
    val name: String? = null,

    @field:Schema(description = "소개글. null 이면 변경 안 함, 빈 문자열이면 비움")
    @field:Size(max = 255)
    val introduce: String? = null,

    @field:Schema(description = "대표 연락처 전체 교체 (1~${Host.MAX_CONTACT_COUNT}개). null 이면 변경 안 함")
    @field:Size(min = 1, max = Host.MAX_CONTACT_COUNT)
    @field:Valid
    val contacts: List<V2HostContactRequest>? = null,

    @field:Schema(description = "프로필 이미지 key (이미지 업로드 API 응답의 key). null 이면 변경 안 함, 빈 문자열이면 기본 이미지")
    @field:Size(max = 255)
    val profileImageKey: String? = null,

    @field:Schema(description = "커버 이미지 key. null 이면 변경 안 함, 빈 문자열이면 기본 이미지")
    @field:Size(max = 255)
    val coverImageKey: String? = null,
)
