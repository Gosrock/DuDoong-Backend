package band.gosrock.domain.domains.host.domain

import band.gosrock.domain.common.aop.domainEvent.Events
import band.gosrock.domain.common.events.host.HostRegisterSlackEvent
import band.gosrock.domain.common.events.host.HostUserInvitationEvent
import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.HostInfoVo
import band.gosrock.domain.common.vo.HostProfileVo
import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException
import band.gosrock.domain.domains.host.exception.CannotModifyMasterHostRoleException
import band.gosrock.domain.domains.host.exception.ForbiddenHostException
import band.gosrock.domain.domains.host.exception.HostUserNotFoundException
import band.gosrock.domain.domains.host.exception.NotAcceptedHostException
import band.gosrock.domain.domains.host.exception.NotManagerHostException
import band.gosrock.domain.domains.host.exception.NotMasterHostException
import band.gosrock.domain.domains.host.exception.NotPartnerHostException
import band.gosrock.domain.domains.host.exception.DuplicateSlackUrlException
import org.apache.commons.codec.binary.StringUtils
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy

@Entity(name = "tbl_host")
class Host(
    // 마스터 유저 id
    var masterUserId: Long? = null,
    // 슬랙 웹훅 url
    var slackUrl: String? = null,
    name: String? = null,
    introduce: String? = null,
    profileImageKey: String? = null,
    contactEmail: String? = null,
    contactNumber: String? = null,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "host_id")
    var id: Long? = null
        protected set

    @Embedded
    var profile: HostProfile? = HostProfile(
        name = name,
        introduce = introduce,
        profileImageKey = profileImageKey,
        contactEmail = contactEmail,
        contactNumber = contactNumber,
    )
        protected set

    // 파트너 여부
    var partner: Boolean = false
        protected set

    // 단방향 oneToMany 매핑
    @OneToMany(
        mappedBy = "host",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER,
    )
    @OrderBy("createdAt DESC")
    val hostUsers: MutableSet<HostUser> = HashSet()

    fun addHostUsers(hostUserList: Set<HostUser>) {
        hostUserList.forEach { validateHostUserExistence(it) }
        this.hostUsers.addAll(hostUserList)
    }

    fun inviteHostUsers(hostUserList: Set<HostUser>) {
        hostUserList.forEach { validateHostUserExistence(it) }
        this.hostUsers.addAll(hostUserList)
        hostUserList.forEach { Events.raise(HostUserInvitationEvent.of(this, it)) }
    }

    fun hasHostUserId(userId: Long): Boolean =
        this.hostUsers.any { it.userId == userId }

    fun hasHostUser(hostUser: HostUser): Boolean = hasHostUserId(hostUser.userId!!)

    fun getHostUserByUserId(userId: Long): HostUser =
        this.hostUsers.firstOrNull { it.userId == userId }
            ?: throw HostUserNotFoundException.EXCEPTION

    fun getHostUser_UserIds(): List<Long> =
        this.hostUsers.mapNotNull { it.userId }

    fun updateProfile(hostProfile: HostProfile) {
        this.profile?.updateProfile(hostProfile)
    }

    fun updateSlackUrl(slackUrl: String) {
        if (StringUtils.equals(this.slackUrl, slackUrl)) throw DuplicateSlackUrlException.EXCEPTION
        Events.raise(HostRegisterSlackEvent.of(this))
        this.slackUrl = slackUrl
    }

    fun isManagerHostUserId(userId: Long): Boolean =
        this.hostUsers.any { it.userId == userId && it.role == HostRole.MANAGER }

    fun isActiveHostUserId(userId: Long): Boolean =
        this.hostUsers.any { it.userId == userId && it.active }

    fun setHostUserRole(userId: Long, role: HostRole) {
        if (this.masterUserId == userId) throw CannotModifyMasterHostRoleException.EXCEPTION
        this.hostUsers.firstOrNull { it.userId == userId }
            ?.setHostRole(role)
            ?: throw HostUserNotFoundException.EXCEPTION
    }

    fun removeHostUser(userId: Long) {
        if (this.isActiveHostUserId(userId)) throw AlreadyJoinedHostException.EXCEPTION
        this.hostUsers.remove(this.getHostUserByUserId(userId))
    }

    /** 해당 유저가 호스트에 이미 속하는지 확인하는 검증 로직입니다 */
    fun validateHostUserIdExistence(userId: Long) {
        if (this.hasHostUserId(userId)) throw AlreadyJoinedHostException.EXCEPTION
    }

    fun validateHostUserExistence(hostUser: HostUser) {
        validateHostUserIdExistence(hostUser.userId!!)
    }

    /** 해당 유저가 호스트에 속하는지 확인하는 검증 로직입니다 */
    fun validateHostUser(userId: Long) {
        if (!this.hasHostUserId(userId)) throw ForbiddenHostException.EXCEPTION
    }

    /** 해당 유저가 호스트에 속하며 가입 승인을 완료했는지 (활성상태) 확인하는 검증 로직입니다 */
    fun validateActiveHostUser(userId: Long) {
        this.validateHostUser(userId)
        if (!this.isActiveHostUserId(userId)) throw NotAcceptedHostException.EXCEPTION
    }

    /** 해당 유저가 매니저 이상인지 확인하는 검증 로직입니다 */
    fun validateManagerHostUser(userId: Long) {
        this.validateActiveHostUser(userId)
        if (!this.isManagerHostUserId(userId) && this.masterUserId != userId) {
            throw NotManagerHostException.EXCEPTION
        }
    }

    /** 해당 유저가 호스트의 마스터(담당자, 방장)인지 확인하는 검증 로직입니다 */
    fun validateMasterHostUser(userId: Long) {
        this.validateActiveHostUser(userId)
        if (this.masterUserId != userId) throw NotMasterHostException.EXCEPTION
    }

    /** 해당 호스트가 파트너 인지 검증합니다. */
    fun validatePartnerHost() {
        if (!partner) throw NotPartnerHostException.EXCEPTION
    }

    /** 마스터 권한을 다른 활성 멤버에게 양도합니다. 현재 마스터만 호출 가능합니다. */
    fun transferMaster(currentMasterUserId: Long, newMasterUserId: Long) {
        validateMasterHostUser(currentMasterUserId)
        validateActiveHostUser(newMasterUserId)
        // 기존 마스터 → MANAGER
        this.hostUsers.first { it.userId == currentMasterUserId }.setHostRole(HostRole.MANAGER)
        // 새 마스터 → MASTER
        this.hostUsers.first { it.userId == newMasterUserId }.setHostRole(HostRole.MASTER)
        this.masterUserId = newMasterUserId
    }

    /** 어드민이 마스터 권한을 강제 양도합니다. 권한 검증 없이 실행됩니다. */
    fun forceTransferMaster(newMasterUserId: Long) {
        validateActiveHostUser(newMasterUserId)
        // 기존 마스터 → MANAGER (있으면)
        this.hostUsers.firstOrNull { it.userId == masterUserId }?.setHostRole(HostRole.MANAGER)
        // 새 마스터 → MASTER
        this.hostUsers.first { it.userId == newMasterUserId }.setHostRole(HostRole.MASTER)
        this.masterUserId = newMasterUserId
    }

    fun isPartnerHost(): Boolean = partner

    fun changePartner(partner: Boolean) {
        this.partner = partner
    }

    fun toHostInfoVo(): HostInfoVo = HostInfoVo.from(this)
    fun toHostProfileVo(): HostProfileVo = HostProfileVo.from(this)
}
