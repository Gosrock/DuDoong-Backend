package band.gosrock.domain.domains.issuedTicket.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
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
    private Long optionGroupId;

    /*
    발급 티켓의 옵션 그룹에 대한 답변들 (양방향)
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers = new ArrayList<>();

    /*
    해당 옵션 그룹이 참조하는 발급 티켓 (양방향)
     */
    @ManyToOne
    @JoinColumn(name = "issued_ticket_id")
    private IssuedTicket issuedTicket;

    @Builder
    public IssuedTicketOptionGroup(
            Long optionGroupId, List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers) {
        this.optionGroupId = optionGroupId;
        this.issuedTicketOptionAnswers = issuedTicketOptionAnswers;
    }
}
