package band.gosrock.api.host.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MASTER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.host.model.dto.request.TransferMasterRequest
import band.gosrock.api.host.model.dto.response.HostDetailResponse
import band.gosrock.api.host.model.mapper.HostMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.repository.HostRepository
import org.springframework.transaction.annotation.Transactional

@UseCase
class TransferMasterUseCase(
    private val hostAdaptor: HostAdaptor,
    private val hostRepository: HostRepository,
    private val hostMapper: HostMapper,
) {
    @Transactional
    @HostRolesAllowed(role = MASTER, findHostFrom = HOST_ID)
    fun execute(userId: Long, hostId: Long, request: TransferMasterRequest): HostDetailResponse {
        val host = hostAdaptor.findById(hostId)
        host.transferMaster(userId, request.newMasterUserId)
        hostRepository.save(host)
        return hostMapper.toHostDetailResponse(host)
    }
}
