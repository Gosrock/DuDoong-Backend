package band.gosrock.admin.service

import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.service.EventService
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminDeleteEventUseCase(
    private val eventAdaptor: EventAdaptor,
    private val eventService: EventService,
) {

    @Transactional
    fun execute(eventId: Long) {
        val event = eventAdaptor.findById(eventId)
        eventService.deleteEventSoft(event)
    }
}
