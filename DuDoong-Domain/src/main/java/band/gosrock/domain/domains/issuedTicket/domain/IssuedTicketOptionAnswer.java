package band.gosrock.domain.domains.issuedTicket.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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

    /*
    옵션에 대한 답변 참조 (단방향)
     */
    private Long optionId;

    /*
    해당 답변이 속한 티켓 옵션 그룹 (양방향)
     */
    @ManyToOne
    @JoinColumn(name = "issued_ticket_option_group_id")
    private IssuedTicketOptionGroup issuedTicketOptionGroup;

    /*
    답변
     */
    private String answer;

    @Builder
    public IssuedTicketOptionAnswer(String answer) {
        this.answer = answer;
    }

    public static IssuedTicketOptionAnswer createIssuedTicketOptionAnswer(String answer) {
        return IssuedTicketOptionAnswer.builder().answer(answer).build();
    }
}
