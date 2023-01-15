package band.gosrock.infrastructure.outer.api.oauth.dto;


import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsUnHandleException;
import band.gosrock.infrastructure.outer.api.tossPayments.exception.TossPaymentsErrorDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import feign.Response;
import java.io.IOException;
import java.io.InputStream;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
public class KakaoKauthErrorResponse {
    private String error;
    private String errorCode;
    private String errorDescription;

    public static KakaoKauthErrorResponse from(Response response) {
        try (InputStream bodyIs = response.body().asInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(bodyIs, KakaoKauthErrorResponse.class);
        } catch (IOException e) {
            throw PaymentsUnHandleException.EXCEPTION;
        }
    }
}
