package band.gosrock.domain.domains.host.domain

import band.gosrock.domain.common.aop.domainEvent.Events
import band.gosrock.domain.common.events.host.HostRegisterSlackEvent
import band.gosrock.domain.common.events.host.HostUserInvitationEvent
import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.HostInfoVo
import band.gosrock.domain.common.vo.HostProfileVo
import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException
import band.gosrock.domain.domains.host.exception.CannotAssignMasterRoleException
import band.gosrock.domain.domains.host.exception.CannotModifyMasterHostRoleException
import band.gosrock.domain.domains.host.exception.ForbiddenHostException
import band.gosrock.domain.domains.host.exception.HostUserNotFoundException
import band.gosrock.domain.domains.host.exception.ManagerCanManageGuestOnlyException
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
import org.hibernate.annotations.BatchSize

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
    @BatchSize(size = 100)
    val hostUsers: MutableSet<HostUser> = HashSet()

    // v2 대표 연락처 (N개). 비어 있으면 v1 contactEmail/contactNumber 로 대체 표시한다 (V2HostDomainService.displayContacts)
    @OneToMany(mappedBy = "host", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    val contacts: MutableList<HostContact> = mutableListOf()

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
        syncContactsFromV1(hostProfile.contactEmail, hostProfile.contactNumber)
    }

    /**
     * v1(호스트 프로필 수정 / 어드민 수정)에서 바뀐 contactEmail / contactNumber 를 v2 연락처에 반영합니다.
     * v2 연락처가 없는 호스트는 조회 시 v1 값으로 대체 표시하므로 건드리지 않습니다.
     * 첫 EMAIL / PHONE 항목 값을 갱신하고, 해당 유형이 없으면 끝에 추가합니다 (최대 개수·길이 초과 시 추가 안 함).
     */
    fun syncContactsFromV1(contactEmail: String?, contactNumber: String?) {
        if (this.contacts.isEmpty()) return
        syncContactFromV1(HostContactType.EMAIL, contactEmail, HostContact.VALUE_MAX_LENGTH)
        syncContactFromV1(HostContactType.PHONE, contactNumber, HostContact.PHONE_MAX_LENGTH)
    }

    private fun syncContactFromV1(type: HostContactType, value: String?, maxLength: Int) {
        if (value.isNullOrBlank() || value.length > maxLength) return
        val existing = this.contacts.firstOrNull { it.type == type }
        if (existing != null) {
            existing.changeValue(value)
            return
        }
        if (this.contacts.size >= MAX_CONTACT_COUNT) return
        val contact = HostContact(type = type, value = value)
        contact.assignTo(this, (this.contacts.maxOfOrNull { it.sortOrder } ?: -1) + 1)
        this.contacts.add(contact)
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

    /**
     * 멤버 추가·초대에서 요청자가 지정할 수 있는 역할인지 검증합니다 (v1·v2 공통).
     * MASTER 는 지정할 수 없고(양도 API 사용), GUEST 가 아닌 역할(MANAGER)은 마스터만 다룰 수 있습니다.
     * (요청자의 매니저 이상 권한 자체는 @HostRolesAllowed 에서 검증)
     */
    fun validateCanManageRole(requesterUserId: Long, targetRole: HostRole) {
        if (targetRole == HostRole.MASTER) throw CannotAssignMasterRoleException.EXCEPTION
        if (targetRole != HostRole.GUEST && this.masterUserId != requesterUserId) {
            throw ManagerCanManageGuestOnlyException.EXCEPTION
        }
    }

    /** 활성 멤버의 역할을 GUEST ↔ MANAGER 로 변경합니다 (v1·v2 공통). MASTER 지정 불가, 초대 대기·비멤버·마스터는 대상이 될 수 없습니다 */
    fun changeActiveHostUserRole(targetUserId: Long, role: HostRole) {
        if (role == HostRole.MASTER) throw CannotAssignMasterRoleException.EXCEPTION
        if (!isActiveHostUserId(targetUserId)) throw HostUserNotFoundException.EXCEPTION
        setHostUserRole(targetUserId, role)
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

    // ===== v2 공유 데이터 (검증·조합 규칙은 service.v2.V2HostDomainService) =====

    /** 활성(초대 수락) 멤버 목록 */
    fun getActiveHostUsers(): List<HostUser> = this.hostUsers.filter { it.active }

    /** 활성 멤버의 역할. 멤버가 아니거나 비활성이면 null */
    fun getActiveRoleOf(userId: Long): HostRole? =
        this.hostUsers.firstOrNull { it.userId == userId && it.active }?.role

    /** 프로필 embeddable 이 없는 호스트면 빈 프로필을 만들어 돌려준다 (v2 부분 수정용) */
    internal fun getOrInitProfile(): HostProfile = this.profile ?: HostProfile().also { this.profile = it }

    /**
     * 연락처 전체 교체. 개수·길이 검증은 V2HostDomainService 에서 한다.
     * v1 호환: 첫 EMAIL → contactEmail, 첫 PHONE → contactNumber 에도 기록한다. 해당 유형이 없으면 기존 v1 값을 유지한다.
     */
    internal fun replaceContacts(newContacts: List<HostContact>) {
        this.contacts.clear()
        newContacts.forEachIndexed { index, contact ->
            contact.assignTo(this, index)
            this.contacts.add(contact)
        }
        val profile = getOrInitProfile()
        newContacts.firstOrNull { it.type == HostContactType.EMAIL }?.let { profile.contactEmail = it.value }
        newContacts.firstOrNull { it.type == HostContactType.PHONE }?.let { profile.contactNumber = it.value }
    }

    fun toHostInfoVo(): HostInfoVo = HostInfoVo.from(this)
    fun toHostProfileVo(): HostProfileVo = HostProfileVo.from(this)

    companion object {
        const val MAX_CONTACT_COUNT = 10
    }
}
