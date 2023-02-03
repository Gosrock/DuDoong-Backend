package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IssuedTicketOptionAnswerVo {

    private final Long issuedTicketOptionAnswerId;

    private final String optionQuestion;

    private final String answer;

    private final Money additionalPrice;

    public static IssuedTicketOptionAnswerVo from(
            IssuedTicketOptionAnswer issuedTicketOptionAnswer) {
        return IssuedTicketOptionAnswerVo.builder()
                .issuedTicketOptionAnswerId(issuedTicketOptionAnswer.getId())
                .answer(issuedTicketOptionAnswer.getAnswer())
                .additionalPrice(issuedTicketOptionAnswer.getAdditionalPrice())
                .build();
    }
}
