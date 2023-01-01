package io.github.depromeet.knockknockbackend.global.utils.api.dto.response;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoInformationResponse {

    private Properties properties;
    private String id;

    private KakaoAccount kakaoAccount;

    @Getter
    @NoArgsConstructor
    public static class Properties {
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    public static class KakaoAccount {

        //        private Profile profile;
        private String email;
        //        @Getter
        //        @NoArgsConstructor
        //        public static class Profile {
        //            @JsonProperty("profile_image_url")
        //            private String profileImageUrl;
        //        }

        //        public String getProfileImageUrl() {
        //            return profile.getProfileImageUrl();
        //        }

    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return kakaoAccount.getEmail();
    }

    //    public String getName() {
    //        return properties.getNickname();
    //    }
    //
    //    public String getProfileImage() {
    //        return kakaoAccount.getProfileImageUrl();
    //    }

}
