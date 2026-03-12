package band.gosrock.domain.domains.order.repository.condition

import com.querydsl.core.types.dsl.BooleanExpression

class FindEventOrdersCondition @JvmOverloads constructor(
    val eventId: Long,
    searchString: String? = null,
    val searchType: AdminTableSearchType?,
    val filterType: AdminTableOrderFilterType,
) {
    val searchString: String = searchString ?: ""

    fun getOrderStatusFilter(): BooleanExpression = filterType.getFilter()

    fun showDeleteUserExpression(): BooleanExpression? = filterType.showDeleteUserExpression()

    fun getSearchStringFilter(): BooleanExpression? {
        if (searchType == null) return null
        return searchType.getContains(searchString)
    }
}
