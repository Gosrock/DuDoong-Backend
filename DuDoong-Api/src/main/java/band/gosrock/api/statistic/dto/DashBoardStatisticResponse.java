package band.gosrock.api.statistic.dto;


import band.gosrock.api.statistic.query.result.IssuedTicketStatistic;
import band.gosrock.api.statistic.query.result.OrderStatistic;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;

@Getter
public class DashBoardStatisticResponse {

    @JsonUnwrapped OrderStatistic orderStatistic;

    @JsonUnwrapped IssuedTicketStatistic issuedTicketStatistic;

    @Builder
    public DashBoardStatisticResponse(
            OrderStatistic orderStatistic, IssuedTicketStatistic issuedTicketStatistic) {
        this.orderStatistic = orderStatistic;
        this.issuedTicketStatistic = issuedTicketStatistic;
    }

    public static DashBoardStatisticResponse of(
            OrderStatistic orderStatistic, IssuedTicketStatistic issuedTicketStatistic) {
        return DashBoardStatisticResponse.builder()
                .orderStatistic(orderStatistic)
                .issuedTicketStatistic(issuedTicketStatistic)
                .build();
    }
}
