package band.gosrock.domain.common.vo;


import band.gosrock.domain.common.util.PhoneNumberInstance;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneNumberVo {

    // +1
    private String countryCode;
    // 2015550123
    private String nationalNumber;

    @Builder
    public PhoneNumberVo(String countryCode, String nationalNumber) {
        this.countryCode = countryCode;
        this.nationalNumber = nationalNumber;
    }

    public PhoneNumberVo valueOf(String rawPhoneNumber) throws NumberParseException {
        PhoneNumberUtil instance = PhoneNumberInstance.instance;
        PhoneNumber phoneNumber = instance.parse(rawPhoneNumber, "KR");

        return PhoneNumberVo.builder().nationalNumber(
            String.valueOf(phoneNumber.getNationalNumber())).countryCode(
            String.valueOf(phoneNumber.getCountryCode())).build();
    }

    public String getNumberToParse() {
        return countryCode + " " + nationalNumber;
    }

    /**
     * 010-xxxx-xxxx format
     */
    public String getNationalFormat() throws NumberParseException {
        PhoneNumberUtil instance = PhoneNumberInstance.instance;
        PhoneNumber phoneNumber = instance.parse(getNumberToParse(), "KR");
        return instance.format(phoneNumber, PhoneNumberFormat.NATIONAL);
    }

    /**
     * +82 10-xxxx-xxxx format
     */
    public String getInternationalFormat() throws NumberParseException {
        PhoneNumberUtil instance = PhoneNumberInstance.instance;
        PhoneNumber phoneNumber = instance.parse(getNumberToParse(), "KR");
        return instance.format(phoneNumber, PhoneNumberFormat.INTERNATIONAL);
    }

    /**
     * 01000000000 format
     */
    public String getNaverSmsToNumber() throws NumberParseException {
        String nationalFormat = this.getNationalFormat();
        return nationalFormat.replaceAll("-","");
    }
}
