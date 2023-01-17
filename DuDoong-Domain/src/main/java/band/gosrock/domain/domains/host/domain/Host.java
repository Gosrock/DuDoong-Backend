package band.gosrock.domain.domains.host.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_host")
public class Host extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "host_id")
    private Long id;

    // 대표자 이메일
    private String contactEmail;

    // 대표자 연락처
    @Column(length = 15)
    private String contactNumber;

    // 마스터 유저 id
    private Long masterUserId;

    // 파트너 여부
    private Boolean partner;

    //         단방향 oneToMany 매핑
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "host_user_id")
    private List<HostUser> hostUsers = new ArrayList<>();


    public void addHostUsers(List<HostUser> hostUserList) {
        hostUsers.addAll(hostUserList);
    }


    @Builder
    public Host(
            String contactEmail,
            String contactNumber,
            Long masterUserId,
            Boolean partner
//            List<HostUser> hostUsers
    ) {
        this.contactEmail = contactEmail;
        this.contactNumber = contactNumber;
        this.masterUserId = masterUserId;
        this.partner = partner;
        //        this.hostUsers = hostUsers;
    }
}
