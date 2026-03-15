package band.gosrock.api.host.model.dto.response

import band.gosrock.domain.common.vo.EventProfileVo
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.host.domain.Host
import com.fasterxml.jackson.annotation.JsonUnwrapped

data class HostEventProfileResponse(
    val hostId: Long?,
    val hostName: String?,
    @field:JsonUnwrapped val eventProfileVo: EventProfileVo,
) {
    companion object {
        fun of(host: Host, event: Event): HostEventProfileResponse {
            return HostEventProfileResponse(
                hostId = host.id,
                hostName = host.profile!!.name,
                eventProfileVo = event.toEventProfileVo(),
            )
        }
    }
}
