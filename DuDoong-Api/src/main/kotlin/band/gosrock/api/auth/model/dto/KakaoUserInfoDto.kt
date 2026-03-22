package band.gosrock.api.auth.model.dto

import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.OauthProvider
import band.gosrock.domain.domains.user.domain.Profile

data class KakaoUserInfoDto(
    val oauthId: String,
    val email: String?,
    val phoneNumber: String?,
    val profileImage: String?,
    val name: String?,
    val oauthProvider: OauthProvider
) {
    fun toProfile(): Profile = Profile(
        profileImage = profileImage,
        phoneNumber = phoneNumber,
        name = name ?: "",
        email = email ?: "",
    )

    fun toOauthInfo(): OauthInfo = OauthInfo(
        oid = oauthId,
        provider = oauthProvider,
    )
}
