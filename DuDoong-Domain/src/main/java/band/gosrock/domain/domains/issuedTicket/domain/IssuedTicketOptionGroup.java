package band.gosrock.domain.domains.issuedTicket.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_issued_ticket_option_group")
public class IssuedTicketOptionGroup extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issued_ticket_option_group_id")
    private Long id;

    /*
    옵션 그룹 참조 (단방향)
     */
    @ManyToOne
    @JoinColumn(name = "option_group_id")
    private OptionGroup optionGroup;

    //    public void setOptionGroup(OptionGroup optionGroup) {
    //        this.optionGroup = optionGroup;
    //    }

    /*
    발급 티켓의 옵션 그룹에 대한 답변들 (양방향)
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers = new ArrayList<>();

    public void insertIssuedTicketOptionAnswers(IssuedTicketOptionAnswer issuedTicketOptionAnswer) {
        issuedTicketOptionAnswers.add(issuedTicketOptionAnswer);
    }

    /*
    해당 옵션 그룹이 참조하는 발급 티켓 (양방향)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_ticket_id")
    private IssuedTicket issuedTicket;

    //    public void setIssuedTicket(IssuedTicket issuedTicket) {
    //        this.issuedTicket = issuedTicket;
    //    }

    @Builder
    public IssuedTicketOptionGroup(
            OptionGroup optionGroup,
            IssuedTicket issuedTicket,
            List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers) {
        this.optionGroup = optionGroup;
        this.issuedTicket = issuedTicket;
        this.issuedTicketOptionAnswers = issuedTicketOptionAnswers;
    }

    public static IssuedTicketOptionGroup create(
            OptionGroup optionGroup, IssuedTicket issuedTicket) {
        return IssuedTicketOptionGroup.builder()
                .optionGroup(optionGroup)
                .issuedTicket(issuedTicket)
                .issuedTicketOptionAnswers(new ArrayList<>())
                .build();
    }
}
