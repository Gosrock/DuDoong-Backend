package band.gosrock.domain.domains.issuedTicket.dto.request;


import java.util.List;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class CreateIssuedTicketForDevDTO {

    @NotNull private Long eventId;

    @NotNull private Long orderLineId;

    @NotNull private Long ticketItemId;

    @NotNull private Long amount;

    @NotNull private List<Long> optionAnswers;
}
