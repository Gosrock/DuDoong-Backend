package band.gosrock.domain.domains.host.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import java.util.HashSet;
import java.util.Set;
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
    private Set<HostUser> hostUsers = new HashSet<>();

    public void addHostUsers(Set<HostUser> hostUserList) {
        this.hostUsers.addAll(hostUserList);
    }

    public Boolean hasHostUserId(Long userId) {
        return this.hostUsers.stream().anyMatch(hostUser -> hostUser.getUserId().equals(userId));
    }

    public Boolean isSuperHostUserId(Long userId) {
        return this.hostUsers.stream().anyMatch(hostUser -> hostUser.getUserId().equals(userId) && hostUser.getRole().equals(HostRole.SUPER_HOST));
    }

    @Builder
    public Host(String contactEmail, String contactNumber, Long masterUserId) {
        this.contactEmail = contactEmail;
        this.contactNumber = contactNumber;
        this.masterUserId = masterUserId;
        this.partner = false; // 정책상 초기값 false 로 고정입니다
    }
}
