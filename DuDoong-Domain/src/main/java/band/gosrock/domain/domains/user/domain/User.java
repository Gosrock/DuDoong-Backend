package band.gosrock.domain.domains.user.domain;


import band.gosrock.domain.common.aop.domainEvent.Events;
import band.gosrock.domain.common.events.user.UserRegisterEvent;
import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.domains.user.exception.AlreadyDeletedUserException;
import band.gosrock.domain.domains.user.exception.ForbiddenUserException;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
    @EmbeddedId private UserId id;
    @Embedded private Profile profile;
    @Embedded private OauthInfo oauthInfo;

    @Enumerated(EnumType.STRING)
    private AccountState accountState = AccountState.NORMAL;

    @Enumerated(EnumType.STRING)
    private AccountRole accountRole = AccountRole.USER;

    @Builder
    public User(Profile profile, OauthInfo oauthInfo) {
        this.profile = profile;
        this.oauthInfo = oauthInfo;
    }

    @PostPersist
    public void registerEvent() {
        UserRegisterEvent userRegisterEvent = UserRegisterEvent.builder().userId(this.id).build();
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
        oauthInfo = oauthInfo.withDrawOauthInfo();
    }

    public void login() {
        if (!AccountState.NORMAL.equals(this.accountState)) {
            throw ForbiddenUserException.EXCEPTION;
        }
    }
}
