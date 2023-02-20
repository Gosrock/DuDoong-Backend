package band.gosrock.domain.domains.settlement.domain;


import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.FeeCode;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.SettlementFeeDto;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementFeeVo {
    @Enumerated(EnumType.STRING)
    private FeeCode type;

    @Column(name = "fee")
    private Long fee;

    @Builder
    public SettlementFeeVo(FeeCode type, Long fee) {
        this.type = type;
        this.fee = fee;
    }

    public static SettlementFeeVo from(SettlementFeeDto settlementFeeDto) {
        return SettlementFeeVo.builder()
                .fee(settlementFeeDto.getFee())
                .type(settlementFeeDto.getType())
                .build();
    }
}
