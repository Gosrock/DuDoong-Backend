package band.gosrock.domain.domains.order.repository.condition;


import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.Builder;
import lombok.Getter;

@Getter
public class FindEventOrdersCondition {
    private final Long eventId;
    //
    private final String searchString;

    private final AdminTableSearchType searchType;

    private final AdminTableOrderFilterType filterType;

    @Builder
    public FindEventOrdersCondition(
            Long eventId,
            String searchString,
            AdminTableSearchType searchType,
            AdminTableOrderFilterType filterType) {
        this.eventId = eventId;
        this.searchString = searchString != null ? searchString : "";
        this.searchType = searchType;
        this.filterType = filterType;
    }

    public BooleanExpression getOrderStatusFilter() {
        return filterType.getFilter();
    }

    public BooleanExpression getSearchStringFilter() {
        if (searchType == null) return null;
        return searchType.getContains(searchString);
    }
}
