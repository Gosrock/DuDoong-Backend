package band.gosrock.api.statistic.query.result

class IssuedTicketStatistic(issuedCount: Long, enteredCount: Long) {
    val issuedCount: Long = issuedCount
    val enteredCount: Long = enteredCount
    val notEnteredCount: Long = issuedCount - enteredCount
}
