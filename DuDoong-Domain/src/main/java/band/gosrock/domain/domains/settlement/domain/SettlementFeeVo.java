package band.gosrock.domain.domains.settlement.domain;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.FeeCode;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.SettlementFeeDto;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementFeeVo {
    @Enumerated(EnumType.STRING)
    private FeeCode type;

    private Money fee;

    @Builder
    public SettlementFeeVo(FeeCode type, Money fee) {
        this.type = type;
        this.fee = fee;
    }

    public static SettlementFeeVo from(SettlementFeeDto settlementFeeDto) {
        return SettlementFeeVo.builder()
                .fee(Money.wons(settlementFeeDto.getFee()))
                .type(settlementFeeDto.getType())
                .build();
    }
}
