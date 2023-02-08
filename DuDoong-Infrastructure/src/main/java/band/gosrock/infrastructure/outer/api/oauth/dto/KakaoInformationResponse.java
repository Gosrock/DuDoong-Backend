package band.gosrock.infrastructure.outer.api.oauth.dto;


import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
public class KakaoInformationResponse {

    private Properties properties;
    private String id;

    private KakaoAccount kakaoAccount;

    @Getter
    @NoArgsConstructor
    @JsonNaming(SnakeCaseStrategy.class)
    public static class Properties {
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    @JsonNaming(SnakeCaseStrategy.class)
    public static class KakaoAccount {

        private Profile profile;
        private String email;
        private String phoneNumber;
        private String name;

        @Getter
        @NoArgsConstructor
        @JsonNaming(SnakeCaseStrategy.class)
        public static class Profile {
            private String profileImageUrl;
        }

        public String getProfileImageUrl() {
            return profile.getProfileImageUrl();
        }
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return kakaoAccount.getEmail();
    }

    public String getPhoneNumber() {
        return kakaoAccount.getPhoneNumber();
    }

    public String getName() {
        return kakaoAccount.getName() != null ? kakaoAccount.getName() : properties.getNickname();
    }

    public String getProfileUrl() {
        return kakaoAccount.getProfileImageUrl();
    }

    public static void main(String[] args) throws NumberParseException {
        String s = "82 10-9476-8640";
        PhoneNumberUtil instance = PhoneNumberUtil.getInstance();

        PhoneNumber phoneNumber = instance.parseAndKeepRawInput(s,"KR");
        System.out.println(instance.getExampleNumber("US"));

        System.out.println(phoneNumber.getCountryCode());
        System.out.println(phoneNumber.getNationalNumber());
        System.out.println(phoneNumber.getCountryCodeSource());
        System.out.println(phoneNumber.getExtension());
        System.out.println(phoneNumber.getNumberOfLeadingZeros());
        System.out.println(phoneNumber.getCountryCode());
        System.out.println(phoneNumber.getPreferredDomesticCarrierCode());
//        PhoneNumberUtil.
        String format = instance.format(phoneNumber, PhoneNumberFormat.NATIONAL);
        System.out.println(format);

//        PhoneNumber p  =instance.
    }
}
