package band.gosrock.api.statistic.query.result;


import band.gosrock.domain.common.vo.Money;
import java.math.BigDecimal;
import lombok.Getter;

@Getter
public class OrderStatistic {

    private final Long notApprovedCount;
    private final Long DoneCount;
    private final Money sellAmount;

    public OrderStatistic(Long DoneCount, Long notApprovedCount, BigDecimal sellAmount) {
        this.notApprovedCount = notApprovedCount;
        this.DoneCount = DoneCount;
        this.sellAmount = Money.wons(sellAmount.longValue());
    }
}
