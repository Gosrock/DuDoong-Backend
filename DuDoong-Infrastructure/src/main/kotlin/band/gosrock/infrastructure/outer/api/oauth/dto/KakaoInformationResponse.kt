package band.gosrock.infrastructure.outer.api.oauth.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(SnakeCaseStrategy::class)
class KakaoInformationResponse {

    var properties: Properties? = null
    var id: String? = null
    var kakaoAccount: KakaoAccount? = null

    @JsonNaming(SnakeCaseStrategy::class)
    class Properties {
        var nickname: String? = null
    }

    @JsonNaming(SnakeCaseStrategy::class)
    class KakaoAccount {
        var profile: Profile? = null
        var email: String? = null
        var phoneNumber: String? = null
        var name: String? = null

        @JsonNaming(SnakeCaseStrategy::class)
        class Profile {
            var profileImageUrl: String? = null
        }

        fun getProfileImageUrl(): String? = profile?.profileImageUrl
    }

    fun getEmail(): String? = kakaoAccount?.email

    fun getPhoneNumber(): String? = kakaoAccount?.phoneNumber

    fun getName(): String? = kakaoAccount?.name ?: properties?.nickname

    fun getProfileUrl(): String? = kakaoAccount?.getProfileImageUrl()
}
