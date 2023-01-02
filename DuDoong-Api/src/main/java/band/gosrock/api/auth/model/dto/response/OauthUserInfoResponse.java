package band.gosrock.api.auth.model.dto.response;


import band.gosrock.api.auth.model.dto.KakaoUserInfoDto;
import lombok.Builder;
import lombok.Getter;

@Getter
public class OauthUserInfoResponse {
    private final String email;
    private final String phoneNumber;
    private final String profileImage;
    private final String name;

    @Builder
    public OauthUserInfoResponse(
            String email, String phoneNumber, String profileImage, String name) {
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.profileImage = profileImage;
        this.name = name;
    }

    public static OauthUserInfoResponse from(KakaoUserInfoDto kakaoUserInfoDto) {
        return OauthUserInfoResponse.builder()
                .email(kakaoUserInfoDto.getEmail())
                .phoneNumber(kakaoUserInfoDto.getPhoneNumber())
                .profileImage(kakaoUserInfoDto.getProfileImage())
                .name(kakaoUserInfoDto.getName())
                .build();
    }
}
