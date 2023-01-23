package band.gosrock.domain.domains.host.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.domains.host.exception.ForbiddenHostOperationException;
import band.gosrock.domain.domains.host.exception.HostUserNotFoundException;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

import band.gosrock.domain.domains.host.exception.NotMasterHostException;
import band.gosrock.domain.domains.host.exception.NotSuperHostException;
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

    @Embedded private HostProfile profile;

    // 마스터 유저 id
    private Long masterUserId;

    // 파트너 여부
    private Boolean partner;

    // 슬랙 웹훅 url
    private String slackUrl;

    // 단방향 oneToMany 매핑
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private final Set<HostUser> hostUsers = new HashSet<>();

    public void addHostUsers(Set<HostUser> hostUserList) {
        this.hostUsers.addAll(hostUserList);
    }

    public Boolean hasHostUserId(Long userId) {
        return this.hostUsers.stream().anyMatch(hostUser -> hostUser.getUserId().equals(userId));
    }

    public HostUser getHostUserByUserId(Long userId) {
        return this.hostUsers.stream()
                .filter(hostUser -> hostUser.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    public void setProfile(HostProfile hostProfile) {
        this.profile = hostProfile;
    }

    public void setSlackUrl(String slackUrl) {
        this.slackUrl = slackUrl;
    }

    public Boolean isSuperHostUserId(Long userId) {
        return this.hostUsers.stream()
                .anyMatch(
                        hostUser ->
                                hostUser.getUserId().equals(userId)
                                        && hostUser.getRole().equals(HostRole.SUPER_HOST));
    }

    public void setHostUserRole(Long userId, HostRole role) {
        // 마스터의 역할은 수정할 수 없음
        if (this.getMasterUserId().equals(userId)) {
            throw ForbiddenHostOperationException.EXCEPTION;
        }
        this.hostUsers.stream()
                .filter(hostUser -> hostUser.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> HostUserNotFoundException.EXCEPTION)
                .setHostRole(role);
    }

    /** 해당 유저가 슈퍼 호스트인지 확인하는 검증 로직입니다 */
    public void validateSuperHostUser(Long userId) {
        if (!this.isSuperHostUserId(userId)) {
            throw NotSuperHostException.EXCEPTION;
        }
    }

    /** 해당 유저가 호스트의 마스터(담당자, 방장)인지 확인하는 검증 로직입니다 */
    public void validateMasterHostUser(Long userId) {
        if (!this.getMasterUserId().equals(userId)) {
            throw NotMasterHostException.EXCEPTION;
        }
    }

    @Builder
    public Host(
            String name,
            String introduce,
            String since,
            String profileImageUrl,
            String contactEmail,
            String contactNumber,
            String slackUrl,
            Long masterUserId) {
        this.profile =
                HostProfile.builder()
                        .name(name)
                        .introduce(introduce)
                        .since(since)
                        .profileImageUrl(profileImageUrl)
                        .contactEmail(contactEmail)
                        .contactNumber(contactNumber)
                        .build();
        this.masterUserId = masterUserId;
        this.slackUrl = slackUrl;
        this.partner = false; // 정책상 초기값 false 로 고정입니다
    }
}
