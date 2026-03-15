package band.gosrock.api.event.model.dto.response

import band.gosrock.domain.common.vo.EventBasicVo
import band.gosrock.domain.common.vo.EventDetailVo
import band.gosrock.domain.common.vo.EventPlaceVo
import band.gosrock.domain.common.vo.HostInfoVo
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.host.domain.Host
import com.fasterxml.jackson.annotation.JsonUnwrapped

/** 이벤트 디테일 응답 DTO */
data class EventDetailResponse(
    val status: EventStatus?,
    val host: HostInfoVo?,
    @JsonUnwrapped val eventBasicVo: EventBasicVo?,
    val place: EventPlaceVo?,
    @JsonUnwrapped val eventDetailVo: EventDetailVo?
) {
    companion object {
        @JvmStatic
        fun of(host: Host, event: Event): EventDetailResponse {
            return EventDetailResponse(
                eventBasicVo = event.toEventBasicVo(),
                eventDetailVo = event.toEventDetailVo(),
                place = event.toEventPlaceVo(),
                host = host.toHostInfoVo(),
                status = event.status
            )
        }
    }
}
