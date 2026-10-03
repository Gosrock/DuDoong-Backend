package band.gosrock.api.v2.host.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.host.dto.response.V2HostMemberResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.service.HostService
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2RemoveHostMemberUseCase(
    private val hostAdaptor: HostAdaptor,
    private val hostService: HostService,
    private val readHostMembersUseCase: V2ReadHostMembersUseCase,
) {
    /**
     * 멤버 삭제 (HostUser 행 삭제). 마스터는 삭제 불가, 매니저 요청자는 GUEST 만.
     * 본인 삭제(나가기)는 이 규칙 조합으로 항상 막힌다: 마스터는 삭제 불가, 매니저는 매니저 삭제 불가, 일반은 M+ 권한 없음.
     */
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    fun execute(userId: Long, hostId: Long, targetUserId: Long): List<V2HostMemberResponse> {
        val host = hostAdaptor.findById(hostId)
        return readHostMembersUseCase.toMemberResponses(hostService.removeMember(host, userId, targetUserId))
    }
}
