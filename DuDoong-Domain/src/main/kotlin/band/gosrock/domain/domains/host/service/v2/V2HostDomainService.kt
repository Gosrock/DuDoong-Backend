package band.gosrock.domain.domains.host.service.v2

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.domainEvent.Events
import band.gosrock.domain.common.vo.HostContactVo
import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostContact
import band.gosrock.domain.domains.host.domain.HostContactType
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException
import band.gosrock.domain.domains.host.exception.CannotAssignMasterRoleException
import band.gosrock.domain.domains.host.exception.CannotRemoveMasterException
import band.gosrock.domain.domains.host.exception.HostUserNotFoundException
import band.gosrock.domain.domains.host.exception.InvalidHostContactException
import band.gosrock.domain.domains.host.exception.ManagerCanManageGuestOnlyException
import band.gosrock.domain.domains.host.repository.HostRepository
import org.springframework.transaction.annotation.Transactional

/**
 * v2 전용 호스트 규칙 (DEC-018). v1 코드는 이 서비스를 호출하지 않는다.
 * v1/v2 공통 불변식(마스터 1명, v1 ↔ v2 연락처 동기화)은 [Host] 에 있다.
 */
@DomainService
@Transactional(readOnly = true)
class V2HostDomainService(
    private val hostRepository: HostRepository,
) {

    /**
     * 프로필 부분 수정 (저장은 호출자). null 인 항목은 변경하지 않는다.
     * introduce / 이미지 key 는 빈 문자열이면 비운다 (기본 이미지로 변경).
     */
    fun updateProfile(host: Host, name: String?, introduce: String?, profileImageKey: String?, coverImageKey: String?) {
        val profile = host.getOrInitProfile()
        name?.let { profile.name = it }
        introduce?.let { profile.introduce = it.ifBlank { null } }
        profileImageKey?.let { profile.profileImage = ImageVo.valueOf(it.ifBlank { null }) }
        coverImageKey?.let { profile.coverImage = ImageVo.valueOf(it.ifBlank { null }) }
    }

    /** 연락처 전체 교체 (저장은 호출자). 1개 이상 [Host.MAX_CONTACT_COUNT]개 이하. v1 contactEmail / contactNumber 동기화는 [Host.replaceContacts] */
    fun replaceContacts(host: Host, newContacts: List<HostContact>) {
        validateContacts(newContacts)
        host.replaceContacts(newContacts)
    }

    private fun validateContacts(newContacts: List<HostContact>) {
        if (newContacts.isEmpty() || newContacts.size > Host.MAX_CONTACT_COUNT) throw InvalidHostContactException.EXCEPTION
        newContacts.forEach {
            val maxLength = if (it.type == HostContactType.PHONE) HostContact.PHONE_MAX_LENGTH else HostContact.VALUE_MAX_LENGTH
            if (it.value.isBlank() || it.value.length > maxLength) throw InvalidHostContactException.EXCEPTION
        }
    }

    /** 연락처 표시. 연락처 테이블이 비어 있는 기존 호스트는 v1 contactNumber / contactEmail 로 대체한다 */
    fun displayContacts(host: Host): List<HostContactVo> {
        if (host.contacts.isNotEmpty()) return host.contacts.map { it.toHostContactVo() }
        return listOfNotNull(
            host.profile?.contactNumber?.takeIf { it.isNotBlank() }?.let { HostContactVo(HostContactType.PHONE, it) },
            host.profile?.contactEmail?.takeIf { it.isNotBlank() }?.let { HostContactVo(HostContactType.EMAIL, it) },
        )
    }

    /**
     * 요청자가 해당 역할의 멤버를 추가/삭제할 수 있는지 검증합니다.
     * MASTER 역할은 지정 대상이 될 수 없고, GUEST 가 아닌 역할(MANAGER)은 마스터만 다룰 수 있습니다.
     * (요청자의 매니저 이상 권한 자체는 @HostRolesAllowed 에서 검증)
     */
    fun validateCanManageRole(host: Host, requesterUserId: Long, targetRole: HostRole) {
        if (targetRole == HostRole.MASTER) throw CannotAssignMasterRoleException.EXCEPTION
        if (targetRole != HostRole.GUEST && host.masterUserId != requesterUserId) {
            throw ManagerCanManageGuestOnlyException.EXCEPTION
        }
    }

    /** 수락 단계 없이 즉시 활성 멤버로 추가합니다 (DEC-015). 이미 멤버(초대 대기 포함)이면 예외. 추가된 사람 알림용 [V2HostMembersAddedEvent] 발행 */
    fun addActiveHostUsers(host: Host, requesterUserId: Long, newHostUsers: List<HostUser>): Host {
        newHostUsers.forEach { validateCanManageRole(host, requesterUserId, it.role) }
        if (newHostUsers.map { it.userId }.distinct().size != newHostUsers.size) {
            throw AlreadyJoinedHostException.EXCEPTION
        }
        newHostUsers.forEach {
            host.validateHostUserExistence(it)
            it.activate()
        }
        host.hostUsers.addAll(newHostUsers)
        Events.raise(V2HostMembersAddedEvent(hostId = host.id!!, userIds = newHostUsers.map { it.userId!! }))
        return hostRepository.save(host)
    }

    /** 활성 멤버의 역할을 GUEST ↔ MANAGER 로 변경합니다. 마스터 대상 / MASTER 지정 불가 */
    fun changeActiveHostUserRole(host: Host, targetUserId: Long, role: HostRole): Host {
        if (role == HostRole.MASTER) throw CannotAssignMasterRoleException.EXCEPTION
        if (!host.isActiveHostUserId(targetUserId)) throw HostUserNotFoundException.EXCEPTION
        host.setHostUserRole(targetUserId, role)
        return hostRepository.save(host)
    }

    /** 멤버를 삭제합니다 (초대 대기 포함). 마스터는 삭제 불가, 매니저 요청자는 GUEST 만 삭제 가능 */
    fun removeMember(host: Host, requesterUserId: Long, targetUserId: Long): Host {
        val target = host.getHostUserByUserId(targetUserId)
        if (host.masterUserId == targetUserId || target.role == HostRole.MASTER) {
            throw CannotRemoveMasterException.EXCEPTION
        }
        validateCanManageRole(host, requesterUserId, target.role)
        host.hostUsers.remove(target)
        return hostRepository.save(host)
    }
}
