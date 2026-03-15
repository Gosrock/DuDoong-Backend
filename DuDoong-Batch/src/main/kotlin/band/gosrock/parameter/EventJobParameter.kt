package band.gosrock.parameter

import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import org.springframework.batch.core.JobParametersInvalidException
import org.springframework.beans.factory.annotation.Value

class EventJobParameter(
    private val eventAdaptor: EventAdaptor,
) {

    private var _event: Event? = null

    @Value("#{jobParameters[eventId]}")
    fun setDate(eventId: Long?) {
        if (eventId == null) {
            throw JobParametersInvalidException("이벤트 아이디가 필요합니다.")
        }
        this._event = eventAdaptor.findById(eventId)
    }

    fun getEvent(): Event = _event!!
}
