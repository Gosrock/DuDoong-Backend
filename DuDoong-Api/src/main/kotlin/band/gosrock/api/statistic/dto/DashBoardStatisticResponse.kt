package band.gosrock.api.statistic.dto

import band.gosrock.api.statistic.query.result.IssuedTicketStatistic
import band.gosrock.api.statistic.query.result.OrderStatistic
import com.fasterxml.jackson.annotation.JsonUnwrapped

class DashBoardStatisticResponse(
    @JsonUnwrapped val orderStatistic: OrderStatistic,
    @JsonUnwrapped val issuedTicketStatistic: IssuedTicketStatistic,
) {
    companion object {
        @JvmStatic
        fun of(
            orderStatistic: OrderStatistic,
            issuedTicketStatistic: IssuedTicketStatistic,
        ): DashBoardStatisticResponse =
            DashBoardStatisticResponse(
                orderStatistic = orderStatistic,
                issuedTicketStatistic = issuedTicketStatistic,
            )
    }
}
