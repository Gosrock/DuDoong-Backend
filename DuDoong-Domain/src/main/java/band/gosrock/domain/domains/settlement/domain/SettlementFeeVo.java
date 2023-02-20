package band.gosrock.domain.domains.settlement.domain;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.FeeCode;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class SettlementFeeVo {
    @Enumerated(EnumType.STRING)
    private FeeCode type;

    private Money fee;
}
