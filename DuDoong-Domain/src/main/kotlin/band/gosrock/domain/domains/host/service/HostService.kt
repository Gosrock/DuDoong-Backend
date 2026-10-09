package band.gosrock.domain.domains.host.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostProfile
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.host.exception.CannotAssignMasterRoleException
import band.gosrock.domain.domains.host.exception.InvalidSlackUrlException
import band.gosrock.domain.domains.host.exception.ManagerCanManageGuestOnlyException
import band.gosrock.domain.domains.host.repository.HostRepository
import org.apache.commons.codec.binary.StringUtils
import org.springframework.transaction.annotation.Transactional

@DomainService
@Transactional(readOnly = true)
open class HostService(
    private val hostRepository: HostRepository,
    private val hostAdaptor: HostAdaptor,
) {
    open fun createHost(host: Host): Host = hostRepository.save(host)

    open fun addHostUser(host: Host, hostUser: HostUser): Host {
        host.addHostUsers(setOf(hostUser))
        return hostRepository.save(host)
    }

    @RedissonLock(LockName = "호스트유저초대", identifier = "id", paramClassType = Host::class)
    open fun inviteHostUser(host: Host, hostUser: HostUser): Host {
        host.inviteHostUsers(setOf(hostUser))
        return hostRepository.save(host)
    }

    open fun updateHostUserRole(host: Host, userId: Long, role: HostRole): Host {
        host.setHostUserRole(userId, role)
        return hostRepository.save(host)
    }

    open fun updateHostProfile(host: Host, profile: HostProfile): Host {
        host.updateProfile(profile)
        return hostRepository.save(host)
    }

    open fun updateHostSlackUrl(host: Host, url: String): Host {
        host.updateSlackUrl(url)
        return hostRepository.save(host)
    }

    open fun activateHostUser(host: Host, userId: Long): Host {
        host.getHostUserByUserId(userId).activate()
        return hostRepository.save(host)
    }

    open fun removeHostUser(host: Host, userId: Long): Host {
        host.removeHostUser(userId)
        return hostRepository.save(host)
    }

    /**
     * v1 멤버 초대·역할 변경에서 지정할 수 있는 역할인지 검증합니다 (v2 와 같은 규칙).
     * MASTER 는 지정할 수 없고(양도 API 사용), GUEST 가 아닌 역할은 마스터만 지정할 수 있습니다.
     */
    fun validateCanAssignRole(host: Host, requesterUserId: Long, role: HostRole) {
        if (role == HostRole.MASTER) throw CannotAssignMasterRoleException.EXCEPTION
        if (role != HostRole.GUEST && host.masterUserId != requesterUserId) {
            throw ManagerCanManageGuestOnlyException.EXCEPTION
        }
    }

    fun validateDuplicatedSlackUrl(host: Host, url: String) {
        if (StringUtils.equals(host.slackUrl, url)) throw InvalidSlackUrlException.EXCEPTION
    }

    /** 해당 유저가 호스트에 속하는지 확인하는 검증 로직입니다 */
    fun validateHostUser(host: Host, userId: Long) = host.validateHostUser(userId)

    /** 해당 유저가 호스트의 마스터(담당자, 방장)인지 확인하는 검증 로직입니다 */
    fun validateMasterHostUser(host: Host, userId: Long) = host.validateMasterHostUser(userId)

    /** 해당 유저가 슈퍼 호스트인지 확인하는 검증 로직입니다 */
    fun validateManagerHostUser(hostId: Long, userId: Long) {
        val host = hostAdaptor.findById(hostId)
        host.validateManagerHostUser(userId)
    }
}
