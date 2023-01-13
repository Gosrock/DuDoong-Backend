package band.gosrock.infrastructure.outer.api.tossPayments.dto.response;


import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsEnumNotMatchException;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EasyPayCode {
    TOSSPAY("토스페이", "TOSSPAY"),
    NAVERPAY("네이버페이", "NAVERPAY"),
    SAMSUNGPAY("삼성페이", "SAMSUNGPAY"),
    LPAY("엘페이", "LPAY"),
    KAKAOPAY("카카오페이", "KAKAOPAY"),
    PAYCO("페이코", "PAYCO"),
    LGPAY("LG페이", "LGPAY"),
    SSG("SSG페이", "SSG");

    private String kr;
    private String en;

    @JsonCreator
    static EasyPayCode findValue(String code) {
        return Arrays.stream(EasyPayCode.values())
                .filter(EasyPayCode -> EasyPayCode.getKr().equals(code))
                .findFirst()
                .orElseThrow(() -> PaymentsEnumNotMatchException.EXCEPTION);
    }
}
