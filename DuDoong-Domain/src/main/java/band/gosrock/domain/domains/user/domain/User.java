package band.gosrock.domain.domains.user.domain;


import band.gosrock.domain.common.aop.domainEvent.Events;
import band.gosrock.domain.common.events.user.UserRegisterEvent;
import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.UserInfoVo;
import band.gosrock.domain.common.vo.UserProfileVo;
import band.gosrock.domain.domains.user.exception.AlreadyDeletedUserException;
import band.gosrock.domain.domains.user.exception.EmptyPhoneNumException;
import band.gosrock.domain.domains.user.exception.ForbiddenUserException;
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkUserInfo;
import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo;
import com.google.i18n.phonenumbers.NumberParseException;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Table(
        name = "tbl_user",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"oid", "provider"})})
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Embedded private Profile profile;
    @Embedded private OauthInfo oauthInfo;

    @Enumerated(EnumType.STRING)
    private AccountState accountState = AccountState.NORMAL;

    @Enumerated(EnumType.STRING)
    private AccountRole accountRole = AccountRole.USER;

    // 이메일 수신 여부
    private Boolean receiveMail = Boolean.TRUE;

    // 마케팅 동의 여부
    private Boolean marketingAgree = Boolean.FALSE;

    private LocalDateTime lastLoginAt = LocalDateTime.now();

    @Builder
    public User(Profile profile, OauthInfo oauthInfo, Boolean marketingAgree) {
        this.profile = profile;
        this.oauthInfo = oauthInfo;
        this.marketingAgree = marketingAgree;
    }

    @PostPersist
    public void registerEvent() {
        UserRegisterEvent userRegisterEvent = UserRegisterEvent.builder().userId(id).build();
        Events.raise(userRegisterEvent);
    }

    public void changeProfile(final Profile newProfile) {
        profile = newProfile;
    }

    public void withDrawUser() {
        if (accountState.equals(AccountState.DELETED)) {
            throw AlreadyDeletedUserException.EXCEPTION;
        }
        accountState = AccountState.DELETED;
        profile.withdraw();
        oauthInfo = oauthInfo.withDrawOauthInfo();
        marketingAgree = Boolean.FALSE;
        receiveMail = Boolean.FALSE;
    }

    public void login() {
        if (!AccountState.NORMAL.equals(this.accountState)) {
            throw ForbiddenUserException.EXCEPTION;
        }
        lastLoginAt = LocalDateTime.now();
    }

    public UserInfoVo toUserInfoVo() {
        return UserInfoVo.from(this);
    }

    public UserProfileVo toUserProfileVo() {
        return UserProfileVo.from(this);
    }

    public EmailUserInfo toEmailUserInfo() {
        return new EmailUserInfo(profile.getName(), profile.getEmail(), receiveMail);
    }

    public AlimTalkUserInfo toAlimTalkUserInfo() throws NumberParseException {
        if (profile.getPhoneNumberVo() == null) {
            throw EmptyPhoneNumException.EXCEPTION;
        }
        return new AlimTalkUserInfo(
                profile.getName(), profile.getPhoneNumberVo().getNaverSmsToNumber());
    }

    public Boolean isReceiveEmail() {
        return receiveMail;
    }

    public Boolean isAgreeMarketing() {
        return marketingAgree;
    }

    public void toggleReceiveEmail() {
        receiveMail = !receiveMail;
    }

    public void toggleMarketingAgree() {
        marketingAgree = !marketingAgree;
    }

    public Boolean isDeletedUser() {
        return accountState == AccountState.DELETED;
    }
}
