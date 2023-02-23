package band.gosrock.api.issuedTicket.dto.request;


import band.gosrock.domain.domains.issuedTicket.repository.condition.FindEventIssuedTicketsCondition;
import band.gosrock.domain.domains.order.repository.condition.AdminTableSearchType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminIssuedTicketTableQueryRequest {
    private AdminTableSearchType searchType;

    @Schema(nullable = true)
    private String searchString;

    @JsonIgnore
    public FindEventIssuedTicketsCondition toCondition(Long eventId) {
        return FindEventIssuedTicketsCondition.builder()
                .eventId(eventId)
                .searchString(searchString)
                .searchType(searchType)
                .build();
    }
}
