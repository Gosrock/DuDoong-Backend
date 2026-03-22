package band.gosrock.domain.domains.user.domain

import band.gosrock.domain.common.aop.domainEvent.Events
import band.gosrock.domain.common.events.user.UserRegisterEvent
import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.UserInfoVo
import band.gosrock.domain.common.vo.UserProfileVo
import band.gosrock.domain.domains.user.exception.AlreadyDeletedUserException
import band.gosrock.domain.domains.user.exception.EmptyPhoneNumException
import band.gosrock.domain.domains.user.exception.ForbiddenUserException
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkUserInfo
import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo
import com.google.i18n.phonenumbers.NumberParseException
import java.time.LocalDateTime
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "tbl_user",
    uniqueConstraints = [UniqueConstraint(columnNames = ["oid", "provider"])],
)
class User(
    @Embedded
    var profile: Profile? = null,

    @Embedded
    var oauthInfo: OauthInfo? = null,

    // 마케팅 동의 여부
    var marketingAgree: Boolean = false,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    var id: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    var accountState: AccountState = AccountState.NORMAL
        protected set

    @Enumerated(EnumType.STRING)
    var accountRole: AccountRole = AccountRole.USER
        protected set

    // 이메일 수신 여부
    var receiveMail: Boolean = true
        protected set

    var lastLoginAt: LocalDateTime = LocalDateTime.now()
        protected set

    @PostPersist
    fun registerEvent() {
        val event = UserRegisterEvent(userId = id)
        Events.raise(event)
    }

    fun changeProfile(newProfile: Profile) {
        profile = newProfile
    }

    fun changeName(newName: String) {
        profile?.changeName(newName)
    }

    fun withDrawUser() {
        if (accountState == AccountState.DELETED) throw AlreadyDeletedUserException.EXCEPTION
        accountState = AccountState.DELETED
        profile?.withdraw()
        oauthInfo = oauthInfo?.withDrawOauthInfo()
        marketingAgree = false
        receiveMail = false
    }

    fun login() {
        if (accountState != AccountState.NORMAL) throw ForbiddenUserException.EXCEPTION
        lastLoginAt = LocalDateTime.now()
    }

    fun toUserInfoVo(): UserInfoVo = UserInfoVo.from(this)
    fun toUserProfileVo(): UserProfileVo = UserProfileVo.from(this)

    fun toEmailUserInfo(): EmailUserInfo =
        EmailUserInfo(profile!!.name ?: "", profile!!.email ?: "", receiveMail)

    @Throws(NumberParseException::class)
    fun toAlimTalkUserInfo(): AlimTalkUserInfo {
        if (profile?.phoneNumberVo == null) throw EmptyPhoneNumException.EXCEPTION
        return AlimTalkUserInfo(profile!!.name ?: "", profile!!.phoneNumberVo!!.getNaverSmsToNumber())
    }

    fun isReceiveEmail(): Boolean = receiveMail
    fun isAgreeMarketing(): Boolean = marketingAgree

    fun toggleReceiveEmail() {
        receiveMail = !receiveMail
    }

    fun toggleMarketingAgree() {
        marketingAgree = !marketingAgree
    }

    fun changeRole(newRole: AccountRole) {
        accountRole = newRole
    }

    fun changeAccountState(newState: AccountState) {
        if (newState == AccountState.DELETED) {
            withDrawUser()
            return
        }
        accountState = newState
    }

    fun isDeletedUser(): Boolean = accountState == AccountState.DELETED
}
