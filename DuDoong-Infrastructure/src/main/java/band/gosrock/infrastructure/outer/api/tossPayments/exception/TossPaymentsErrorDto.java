package band.gosrock.infrastructure.outer.api.tossPayments.exception;


import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import java.io.IOException;
import java.io.InputStream;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TossPaymentsErrorDto {
    private String code;
    private String message;

    public static TossPaymentsErrorDto from(Response response) {
        try (InputStream bodyIs = response.body().asInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(bodyIs, TossPaymentsErrorDto.class);
        } catch (IOException e) {
            throw PaymentsUnHandleException.EXCEPTION;
        }
    }
}
