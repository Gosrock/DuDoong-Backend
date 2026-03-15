package band.gosrock.api.auth.model.dto.response

import band.gosrock.api.auth.model.dto.KakaoUserInfoDto

data class OauthUserInfoResponse(
    val email: String?,
    val phoneNumber: String?,
    val profileImage: String?,
    val name: String?
) {
    companion object {
        fun from(kakaoUserInfoDto: KakaoUserInfoDto): OauthUserInfoResponse =
            OauthUserInfoResponse(
                email = kakaoUserInfoDto.email,
                phoneNumber = kakaoUserInfoDto.phoneNumber,
                profileImage = kakaoUserInfoDto.profileImage,
                name = kakaoUserInfoDto.name
            )
    }
}
