package band.gosrock.infrastructure.outer.api.tossPayments.dto.response;


import lombok.Getter;

@Getter
public class SettlementFeeDto {

    private FeeCode type;
    private Long fee;
}
