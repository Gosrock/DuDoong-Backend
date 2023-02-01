package band.gosrock.api.statistic.query.result;


import lombok.Getter;

@Getter
public class IssuedTicketStatistic {

    private final Long enteredCount;
    private final Long notEnteredCount;
    private final Long issuedCount;

    public IssuedTicketStatistic(Long issuedCount, Long enteredCount) {
        this.enteredCount = enteredCount;
        this.notEnteredCount = issuedCount - enteredCount;
        this.issuedCount = issuedCount;
    }
}
