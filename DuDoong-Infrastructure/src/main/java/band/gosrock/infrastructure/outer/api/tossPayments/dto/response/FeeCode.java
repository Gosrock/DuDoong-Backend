package band.gosrock.infrastructure.outer.api.tossPayments.dto.response;


import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsEnumNotMatchException;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeeCode {
    BASE("BASE", "기본 수수료"),
    INSTALLMENT_DISCOUNT("INSTALLMENT_DISCOUNT", "PG사 부담 수수료"),
    INSTALLMENT("INSTALLMENT", "상점 부담 무이자 할부 수수료"),
    POINT_SAVING("POINT_SAVING", "카드사 포인트 적립 수수료"),
    ETC("ETC", "기본 수수료");

    private String code;
    private String kr;

    @JsonCreator
    static FeeCode findValue(String code) {
        return Arrays.stream(FeeCode.values())
                .filter(cardCode -> cardCode.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> PaymentsEnumNotMatchException.EXCEPTION);
    }
}
