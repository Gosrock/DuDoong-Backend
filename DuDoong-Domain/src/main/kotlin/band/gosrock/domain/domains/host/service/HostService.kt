package band.gosrock.domain.domains.host.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostProfile
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.host.exception.InvalidSlackUrlException
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
        host.setSlackUrl(url)
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
