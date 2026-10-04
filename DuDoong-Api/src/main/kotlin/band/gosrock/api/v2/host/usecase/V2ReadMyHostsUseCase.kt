package band.gosrock.api.v2.host.usecase

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.host.dto.response.V2MyHostResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadMyHostsUseCase(
    private val hostAdaptor: HostAdaptor,
    private val eventAdaptor: EventAdaptor,
) {
    @Transactional(readOnly = true)
    fun execute(userId: Long, keyword: String?, page: Int, size: Int): V2PageResponse<V2MyHostResponse> {
        val hosts = hostAdaptor.queryPageHostsByActiveUserId(userId, keyword?.trim(), PageRequest.of(page, size))
        val eventCounts = eventAdaptor.queryEventCountsByHostIdIn(hosts.content.map { it.id!! })
        return V2PageResponse.of(
            hosts.map { V2MyHostResponse.of(it, userId, eventCounts[it.id] ?: 0L) }
        )
    }
}
