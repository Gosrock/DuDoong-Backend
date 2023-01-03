package band.gosrock.infrastructure.outer.api.tossPayments.exception;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TossPaymentsErrorDto {
    private String code;
    private String message;
}
