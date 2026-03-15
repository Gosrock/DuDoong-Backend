package band.gosrock.domain.domains.issuedTicket.repository.condition

import band.gosrock.domain.domains.order.repository.condition.AdminTableSearchType
import com.querydsl.core.types.dsl.BooleanExpression

class FindEventIssuedTicketsCondition @JvmOverloads constructor(
    val eventId: Long,
    searchString: String? = null,
    val searchType: AdminTableSearchType?,
) {
    val searchString: String = searchString ?: ""

    fun getSearchStringFilter(): BooleanExpression? {
        if (searchType == null) return null
        return searchType.getContains(searchString)
    }
}
