package band.gosrock.domain.domains.issuedTicket.repository.condition;


import band.gosrock.domain.domains.order.repository.condition.AdminTableSearchType;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.Builder;
import lombok.Getter;

@Getter
public class FindEventIssuedTicketsCondition {
    private final Long eventId;
    //
    private final String searchString;

    private final AdminTableSearchType searchType;

    @Builder
    public FindEventIssuedTicketsCondition(
            Long eventId, String searchString, AdminTableSearchType searchType) {
        this.eventId = eventId;
        this.searchString = searchString != null ? searchString : "";
        this.searchType = searchType;
    }

    public BooleanExpression getSearchStringFilter() {
        if (searchType == null) return null;
        return searchType.getContains(searchString);
    }
}
