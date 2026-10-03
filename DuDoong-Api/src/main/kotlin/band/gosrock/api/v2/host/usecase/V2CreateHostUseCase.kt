package band.gosrock.api.v2.host.usecase

import band.gosrock.api.v2.host.dto.request.V2CreateHostRequest
import band.gosrock.api.v2.host.dto.response.V2CreateHostResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.host.service.HostService
import band.gosrock.domain.domains.host.service.v2.V2HostDomainService
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2CreateHostUseCase(
    private val userAdaptor: UserAdaptor,
    private val hostService: HostService,
    private val v2HostDomainService: V2HostDomainService,
) {
    /** 생성자는 마스터(활성)로 등록된다 */
    @Transactional
    fun execute(userId: Long, request: V2CreateHostRequest): V2CreateHostResponse {
        userAdaptor.queryUser(userId)
        val newHost = Host(masterUserId = userId, name = request.name!!.trim(), introduce = request.introduce?.ifBlank { null })
        // 연락처 저장 + v1 contactEmail / contactNumber 동기화
        v2HostDomainService.replaceContacts(newHost, request.contacts!!.map { it.toEntity() })
        val host = hostService.createHost(newHost)
        val master = HostUser(host = host, userId = userId, role = HostRole.MASTER)
        master.activate()
        return V2CreateHostResponse(hostId = hostService.addHostUser(host, master).id!!)
    }
}
