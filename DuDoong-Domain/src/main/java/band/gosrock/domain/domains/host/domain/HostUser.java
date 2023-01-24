package band.gosrock.domain.domains.host.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_host_user")
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"host_id", "user_id"})})
public class HostUser extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "host_user_id")
    private Long id;

    // 소속 호스트 아이디
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private Host host;

    // 소속 호스트를 관리중인 유저 아이디
    @Column(name = "user_id")
    private Long userId;

    // 유저의 권한
    @Enumerated(EnumType.STRING)
    private HostRole role = HostRole.HOST;

    public void setHostRole(HostRole role) {
        this.role = role;
    }

    @Builder
    public HostUser(Host host, Long userId, HostRole role) {
        this.host = host;
        this.userId = userId;
        this.role = role;
    }
}
