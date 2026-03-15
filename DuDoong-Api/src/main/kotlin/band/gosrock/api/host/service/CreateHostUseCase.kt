package band.gosrock.api.host.service

import band.gosrock.api.common.UserUtils
import band.gosrock.api.host.model.dto.request.CreateHostRequest
import band.gosrock.api.host.model.dto.response.HostResponse
import band.gosrock.api.host.model.mapper.HostMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.service.HostService
import org.springframework.transaction.annotation.Transactional

@UseCase
class CreateHostUseCase(
    private val userUtils: UserUtils,
    private val hostService: HostService,
    private val hostMapper: HostMapper,
) {
    @Transactional
    fun execute(createHostRequest: CreateHostRequest): HostResponse {
        // 존재하는 유저인지 검증
        val user = userUtils.getCurrentUser()
        val userId = user.id!!
        // 호스트 생성
        val host = hostService.createHost(hostMapper.toEntity(createHostRequest, userId))
        // 생성한 유저를 마스터 권한으로 등록
        val masterHostUser = hostMapper.toMasterHostUser(host.id!!, userId)
        // 초대 보류 없이 즉시 활성화
        masterHostUser.activate()
        return HostResponse.of(hostService.addHostUser(host, masterHostUser))
    }
}
