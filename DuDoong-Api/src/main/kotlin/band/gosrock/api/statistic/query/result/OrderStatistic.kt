package band.gosrock.api.statistic.query.result

import band.gosrock.domain.common.vo.Money
import java.math.BigDecimal

class OrderStatistic(doneCount: Long, notApprovedCount: Long, sellAmount: BigDecimal) {
    val DoneCount: Long = doneCount
    val notApprovedCount: Long = notApprovedCount
    val sellAmount: Money = Money.wons(sellAmount.toLong())
}
