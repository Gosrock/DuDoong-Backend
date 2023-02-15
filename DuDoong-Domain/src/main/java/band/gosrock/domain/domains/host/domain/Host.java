package band.gosrock.domain.domains.host.domain;


import band.gosrock.domain.common.aop.domainEvent.Events;
import band.gosrock.domain.common.events.host.HostRegisterSlackEvent;
import band.gosrock.domain.common.events.host.HostUserInvitationEvent;
import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.HostInfoVo;
import band.gosrock.domain.common.vo.HostProfileVo;
import band.gosrock.domain.domains.host.exception.*;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.binary.StringUtils;

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
    // 정책상 초기값 false 로 고정입니다
    private final Boolean partner = false;

    // 슬랙 웹훅 url
    private String slackUrl;

    // 단방향 oneToMany 매핑
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private final Set<HostUser> hostUsers = new HashSet<>();

    public void addHostUsers(Set<HostUser> hostUserList) {
        hostUserList.forEach(this::validateHostUserExistence);
        this.hostUsers.addAll(hostUserList);
    }

    public void inviteHostUsers(Set<HostUser> hostUserList) {
        hostUserList.forEach(this::validateHostUserExistence);
        this.hostUsers.addAll(hostUserList);
        hostUserList.forEach(hostUser -> Events.raise(HostUserInvitationEvent.of(this, hostUser)));
    }

    public Boolean hasHostUserId(Long userId) {
        return this.hostUsers.stream().anyMatch(hostUser -> hostUser.getUserId().equals(userId));
    }

    public Boolean hasHostUser(HostUser hostUser) {
        return this.hasHostUserId(hostUser.getUserId());
    }

    public HostUser getHostUserByUserId(Long userId) {
        return this.hostUsers.stream()
                .filter(hostUser -> hostUser.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> HostUserNotFoundException.EXCEPTION);
    }

    public String getSlackToken() {
        if (this.slackUrl == null) return null;
        return this.slackUrl.substring(this.slackUrl.indexOf("https://hooks.slack.com/services/"));
    }

    public void updateProfile(HostProfile hostProfile) {
        this.profile.updateProfile(hostProfile);
    }

    public void setSlackUrl(String slackUrl) {
        if (StringUtils.equals(this.slackUrl, slackUrl)) {
            throw DuplicateSlackUrlException.EXCEPTION;
        }
        Events.raise(HostRegisterSlackEvent.of(this));
        this.slackUrl = slackUrl;
    }

    public Boolean isManagerHostUserId(Long userId) {
        return this.hostUsers.stream()
                .anyMatch(
                        hostUser ->
                                hostUser.getUserId().equals(userId)
                                        && hostUser.getRole().equals(HostRole.MANAGER));
    }

    public Boolean isActiveHostUserId(Long userId) {
        return this.hostUsers.stream()
                .anyMatch(hostUser -> hostUser.getUserId().equals(userId) && hostUser.getActive());
    }

    public void setHostUserRole(Long userId, HostRole role) {
        // 마스터의 역할은 수정할 수 없음
        if (this.getMasterUserId().equals(userId)) {
            throw CannotModifyMasterHostRoleException.EXCEPTION;
        }
        this.hostUsers.stream()
                .filter(hostUser -> hostUser.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> HostUserNotFoundException.EXCEPTION)
                .setHostRole(role);
    }

    /** 해당 유저가 호스트에 이미 속하는지 확인하는 검증 로직입니다 */
    public void validateHostUserIdExistence(Long userId) {
        if (this.hasHostUserId(userId)) {
            throw AlreadyJoinedHostException.EXCEPTION;
        }
    }

    public void validateHostUserExistence(HostUser hostUser) {
        validateHostUserIdExistence(hostUser.getUserId());
    }

    // todo :: 여기서부터 테스트 진행

    /** 해당 유저가 호스트에 속하는지 확인하는 검증 로직입니다 */
    public void validateHostUser(Long userId) {
        if (!this.hasHostUserId(userId)) {
            throw ForbiddenHostException.EXCEPTION;
        }
    }

    /** 해당 유저가 호스트에 속하며 가입 승인을 완료했는지 (활성상태) 확인하는 검증 로직입니다 */
    public void validateActiveHostUser(Long userId) {
        this.validateHostUser(userId);
        if (!this.isActiveHostUserId(userId)) {
            throw NotAcceptedHostException.EXCEPTION;
        }
    }

    /** 해당 유저가 매니저 이상인지 확인하는 검증 로직입니다 */
    public void validateManagerHostUser(Long userId) {
        this.validateActiveHostUser(userId);
        if (!this.isManagerHostUserId(userId) && !this.getMasterUserId().equals(userId)) {
            throw NotManagerHostException.EXCEPTION;
        }
    }

    /** 해당 유저가 호스트의 마스터(담당자, 방장)인지 확인하는 검증 로직입니다 */
    public void validateMasterHostUser(Long userId) {
        this.validateActiveHostUser(userId);
        if (!this.getMasterUserId().equals(userId)) {
            throw NotMasterHostException.EXCEPTION;
        }
    }

    /** 해당 호스트가 파트너 인지 검증합니다. */
    public void validatePartnerHost() {
        if (!partner) {
            throw NotPartnerHostException.EXCEPTION;
        }
    }

    /** 해당 호스트가 파트너인지아닌지 */
    public Boolean isPartnerHost() {
        return partner;
    }

    public HostInfoVo toHostInfoVo() {
        return HostInfoVo.from(this);
    }

    public HostProfileVo toHostProfileVo() {
        return HostProfileVo.from(this);
    }

    @Builder
    public Host(
            String name,
            String introduce,
            String profileImageKey,
            String contactEmail,
            String contactNumber,
            String slackUrl,
            Long masterUserId) {
        this.profile =
                HostProfile.builder()
                        .name(name)
                        .introduce(introduce)
                        .profileImageKey(profileImageKey)
                        .contactEmail(contactEmail)
                        .contactNumber(contactNumber)
                        .build();
        this.masterUserId = masterUserId;
        this.slackUrl = slackUrl;
    }
}
