package band.gosrock.domain.domains.host.domain

import band.gosrock.domain.common.aop.domainEvent.Events
import band.gosrock.domain.common.events.host.HostRegisterSlackEvent
import band.gosrock.domain.common.events.host.HostUserInvitationEvent
import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.HostContactVo
import band.gosrock.domain.common.vo.HostInfoVo
import band.gosrock.domain.common.vo.HostProfileVo
import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException
import band.gosrock.domain.domains.host.exception.CannotAssignMasterRoleException
import band.gosrock.domain.domains.host.exception.CannotModifyMasterHostRoleException
import band.gosrock.domain.domains.host.exception.CannotRemoveMasterException
import band.gosrock.domain.domains.host.exception.ForbiddenHostException
import band.gosrock.domain.domains.host.exception.HostUserNotFoundException
import band.gosrock.domain.domains.host.exception.InvalidHostContactException
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

    // v2 대표 연락처 (N개). 비어 있으면 v1 contactEmail/contactNumber 로 대체 표시한다 (displayContacts)
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

    // ===== v2 =====

    /** 활성(초대 수락) 멤버 목록 */
    fun getActiveHostUsers(): List<HostUser> = this.hostUsers.filter { it.active }

    /** 활성 멤버의 역할. 멤버가 아니거나 비활성이면 null */
    fun getActiveRoleOf(userId: Long): HostRole? =
        this.hostUsers.firstOrNull { it.userId == userId && it.active }?.role

    /**
     * v2 프로필 수정. null 인 항목은 변경하지 않는다.
     * introduce / 이미지 key 는 빈 문자열이면 비운다 (기본 이미지로 변경).
     */
    fun updateProfileV2(name: String?, introduce: String?, profileImageKey: String?, coverImageKey: String?) {
        val profile = this.profile ?: HostProfile().also { this.profile = it }
        name?.let { profile.name = it }
        introduce?.let { profile.introduce = it.ifBlank { null } }
        profileImageKey?.let { profile.profileImage = ImageVo.valueOf(it.ifBlank { null }) }
        coverImageKey?.let { profile.coverImage = ImageVo.valueOf(it.ifBlank { null }) }
    }

    /**
     * 연락처 전체 교체 (v2). 1개 이상 [MAX_CONTACT_COUNT]개 이하.
     * v1 호환: 첫 EMAIL → contactEmail, 첫 PHONE → contactNumber 에도 기록한다. 해당 유형이 없으면 기존 v1 값을 유지한다.
     */
    fun replaceContacts(newContacts: List<HostContact>) {
        validateContacts(newContacts)
        this.contacts.clear()
        newContacts.forEachIndexed { index, contact ->
            contact.assignTo(this, index)
            this.contacts.add(contact)
        }
        val profile = this.profile ?: HostProfile().also { this.profile = it }
        newContacts.firstOrNull { it.type == HostContactType.EMAIL }?.let { profile.contactEmail = it.value }
        newContacts.firstOrNull { it.type == HostContactType.PHONE }?.let { profile.contactNumber = it.value }
    }

    /** v2 연락처 표시. 연락처 테이블이 비어 있는 기존 호스트는 v1 contactNumber / contactEmail 로 대체한다 */
    fun displayContacts(): List<HostContactVo> {
        if (this.contacts.isNotEmpty()) return this.contacts.map { it.toHostContactVo() }
        return listOfNotNull(
            this.profile?.contactNumber?.takeIf { it.isNotBlank() }?.let { HostContactVo(HostContactType.PHONE, it) },
            this.profile?.contactEmail?.takeIf { it.isNotBlank() }?.let { HostContactVo(HostContactType.EMAIL, it) },
        )
    }

    /**
     * 요청자가 해당 역할의 멤버를 추가/삭제할 수 있는지 검증합니다.
     * MASTER 역할은 지정 대상이 될 수 없고, GUEST 가 아닌 역할(MANAGER)은 마스터만 다룰 수 있습니다.
     * (요청자의 매니저 이상 권한 자체는 @HostRolesAllowed 에서 검증)
     */
    fun validateCanManageRole(requesterUserId: Long, targetRole: HostRole) {
        if (targetRole == HostRole.MASTER) throw CannotAssignMasterRoleException.EXCEPTION
        if (targetRole != HostRole.GUEST && this.masterUserId != requesterUserId) {
            throw ManagerCanManageGuestOnlyException.EXCEPTION
        }
    }

    /** 수락 단계 없이 즉시 활성 멤버로 추가합니다 (DEC-015). 이미 멤버(초대 대기 포함)이면 예외 */
    fun addActiveHostUsers(requesterUserId: Long, newHostUsers: List<HostUser>) {
        newHostUsers.forEach { validateCanManageRole(requesterUserId, it.role) }
        if (newHostUsers.map { it.userId }.distinct().size != newHostUsers.size) {
            throw AlreadyJoinedHostException.EXCEPTION
        }
        newHostUsers.forEach {
            validateHostUserExistence(it)
            it.activate()
        }
        this.hostUsers.addAll(newHostUsers)
    }

    /** 활성 멤버의 역할을 GUEST ↔ MANAGER 로 변경합니다. 마스터 대상 / MASTER 지정 불가 */
    fun changeActiveHostUserRole(targetUserId: Long, role: HostRole) {
        if (role == HostRole.MASTER) throw CannotAssignMasterRoleException.EXCEPTION
        if (!isActiveHostUserId(targetUserId)) throw HostUserNotFoundException.EXCEPTION
        setHostUserRole(targetUserId, role)
    }

    /** 멤버를 삭제합니다 (초대 대기 포함). 마스터는 삭제 불가, 매니저 요청자는 GUEST 만 삭제 가능 */
    fun removeMember(requesterUserId: Long, targetUserId: Long) {
        val target = getHostUserByUserId(targetUserId)
        if (this.masterUserId == targetUserId || target.role == HostRole.MASTER) {
            throw CannotRemoveMasterException.EXCEPTION
        }
        validateCanManageRole(requesterUserId, target.role)
        this.hostUsers.remove(target)
    }

    private fun validateContacts(newContacts: List<HostContact>) {
        if (newContacts.isEmpty() || newContacts.size > MAX_CONTACT_COUNT) throw InvalidHostContactException.EXCEPTION
        newContacts.forEach {
            val maxLength = if (it.type == HostContactType.PHONE) HostContact.PHONE_MAX_LENGTH else HostContact.VALUE_MAX_LENGTH
            if (it.value.isBlank() || it.value.length > maxLength) throw InvalidHostContactException.EXCEPTION
        }
    }

    fun toHostInfoVo(): HostInfoVo = HostInfoVo.from(this)
    fun toHostProfileVo(): HostProfileVo = HostProfileVo.from(this)

    companion object {
        const val MAX_CONTACT_COUNT = 10
    }
}
