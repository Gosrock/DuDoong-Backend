package band.gosrock.api.v2.host.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MASTER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.host.dto.request.V2UpdateHostMemberRoleRequest
import band.gosrock.api.v2.host.dto.response.V2HostMemberResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.service.v2.V2HostDomainService
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2UpdateHostMemberRoleUseCase(
    private val hostAdaptor: HostAdaptor,
    private val v2HostDomainService: V2HostDomainService,
    private val readHostMembersUseCase: V2ReadHostMembersUseCase,
) {
    /** GUEST ↔ MANAGER. 마스터 대상 / MASTER 지정 불가 */
    @Transactional
    @HostRolesAllowed(role = MASTER, findHostFrom = HOST_ID)
    fun execute(
        userId: Long,
        hostId: Long,
        targetUserId: Long,
        request: V2UpdateHostMemberRoleRequest,
    ): List<V2HostMemberResponse> {
        val host = hostAdaptor.findByIdForUpdate(hostId)
        return readHostMembersUseCase.toMemberResponses(
            v2HostDomainService.changeActiveHostUserRole(host, targetUserId, request.role!!.domain)
        )
    }
}
