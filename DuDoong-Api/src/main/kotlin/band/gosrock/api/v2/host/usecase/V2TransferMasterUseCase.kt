package band.gosrock.api.v2.host.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MASTER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.host.dto.request.V2TransferMasterRequest
import band.gosrock.api.v2.host.dto.response.V2HostMemberResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.repository.HostRepository
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2TransferMasterUseCase(
    private val hostAdaptor: HostAdaptor,
    private val hostRepository: HostRepository,
    private val readHostMembersUseCase: V2ReadHostMembersUseCase,
) {
    /** 대상은 활성 멤버. 기존 마스터 → MANAGER (Host.transferMaster 재사용) */
    @Transactional
    @HostRolesAllowed(role = MASTER, findHostFrom = HOST_ID)
    fun execute(userId: Long, hostId: Long, request: V2TransferMasterRequest): List<V2HostMemberResponse> {
        val host = hostAdaptor.findById(hostId)
        host.transferMaster(userId, request.userId!!)
        return readHostMembersUseCase.toMemberResponses(hostRepository.save(host))
    }
}
