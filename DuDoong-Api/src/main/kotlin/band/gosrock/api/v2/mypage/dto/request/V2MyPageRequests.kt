package band.gosrock.api.v2.mypage.dto.request

import band.gosrock.infrastructure.config.s3.ImageFileExtension
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * M-2 프로필 부분 수정. null 인 필드는 변경하지 않는다.
 *
 * 닉네임 검증은 v1 `ChangeNameRequest`(`@NotBlank` + `@Size(2, 7)`)와 같다. null 을 '변경 안 함'으로 받으려고 `@NotBlank` 대신
 * 같은 판정의 `@Pattern` 을 쓴다 — `@NotBlank` = Java `String.trim()`(U+0020 이하 문자 제거) 후 길이 > 0 = U+0020 보다 큰 문자가 하나 이상.
 * 두 규칙이 같다는 것은 `V2MyPageNameRuleTest` 가 고정한다
 */
data class V2UpdateMeRequest(
    @field:Schema(description = "닉네임 (2~7자, v1 과 같은 규칙). null 이면 변경 안 함", example = "두둥")
    @field:Pattern(regexp = NOT_BLANK_REGEX, message = "이름을 입력해주세요.")
    @field:Size(min = 2, max = 7, message = "이름은 2~7자여야 합니다.")
    val name: String? = null,

    @field:Schema(description = "프로필 이미지 key (M-3 응답의 key). null 이면 변경 안 함, 빈 문자열이면 기본 이미지")
    @field:Size(max = 255)
    val profileImageKey: String? = null,
) {
    companion object {
        /** `@NotBlank` 와 같은 판정: U+0020 보다 큰 문자가 하나 이상 */
        const val NOT_BLANK_REGEX = "(?s).*[^\\x00-\\x20].*"
    }
}

/** M-3 프로필 이미지 업로드 url 발급 */
data class V2MeImageUploadRequest(
    @field:Schema(description = "확장자 (JPEG / JPG / PNG — 기존 이미지 업로드와 같음)", example = "PNG")
    @field:NotNull
    val extension: ImageFileExtension?,
)
