package band.gosrock.api.event.model.dto.response

import band.gosrock.domain.common.vo.EventProfileVo
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.host.domain.Host
import com.fasterxml.jackson.annotation.JsonUnwrapped

/** 이벤트 프로필만 표시하는 응답 DTO */
data class EventProfileResponse(
    val hostId: Long?,
    val hostName: String?,
    @JsonUnwrapped val eventProfileVo: EventProfileVo?
) {
    companion object {
        @JvmStatic
        fun of(host: Host, event: Event): EventProfileResponse {
            return EventProfileResponse(
                hostId = host.id,
                hostName = host.profile!!.name,
                eventProfileVo = event.toEventProfileVo()
            )
        }
    }
}
