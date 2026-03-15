package band.gosrock.api.host.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.host.model.dto.request.UpdateHostSlackRequest
import band.gosrock.api.host.model.dto.response.HostDetailResponse
import band.gosrock.api.host.model.mapper.HostMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.exception.InvalidSlackUrlException
import band.gosrock.domain.domains.host.service.HostService
import band.gosrock.infrastructure.config.slack.SlackMessageProvider
import java.net.UnknownHostException
import org.springframework.transaction.annotation.Transactional

@UseCase
class UpdateHostSlackUrlUseCase(
    private val hostService: HostService,
    private val hostAdaptor: HostAdaptor,
    private val hostMapper: HostMapper,
    private val slackMessageProvider: SlackMessageProvider,
) {
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    fun execute(hostId: Long, updateHostSlackRequest: UpdateHostSlackRequest): HostDetailResponse {
        val host = hostAdaptor.findById(hostId)
        val slackUrl = updateHostSlackRequest.slackUrl
        hostService.validateDuplicatedSlackUrl(host, slackUrl)

        return try {
            slackMessageProvider.register(slackUrl)
            hostMapper.toHostDetailResponse(hostService.updateHostSlackUrl(host, slackUrl))
        } catch (e: UnknownHostException) {
            throw InvalidSlackUrlException.EXCEPTION
        }
    }
}
