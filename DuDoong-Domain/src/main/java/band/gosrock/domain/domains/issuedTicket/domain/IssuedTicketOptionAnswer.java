package band.gosrock.domain.domains.issuedTicket.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_issued_ticket_option_answer")
public class IssuedTicketOptionAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issued_ticket_option_answer_id")
    private Long id;

    private Long optionId;

    private String answer;

    @Builder
    public IssuedTicketOptionAnswer(String answer) {
        this.answer = answer;
    }

    public static IssuedTicketOptionAnswer createIssuedTicketOptionAnswer(String answer) {
        return IssuedTicketOptionAnswer.builder().answer(answer).build();
    }
}
