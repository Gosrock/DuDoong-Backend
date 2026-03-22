package band.gosrock.admin.service

import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.repository.EventRepository
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminDeleteEventUseCase(
    private val eventAdaptor: EventAdaptor,
    private val eventRepository: EventRepository,
    private val adminAuthValidator: AdminAuthValidator,
) {

    @Transactional
    fun execute(userId: Long, eventId: Long) {
        adminAuthValidator.validateAdminOrAbove(userId)
        val event = eventAdaptor.findById(eventId)
        // 어드민은 밸리데이션 없이 직접 DELETED 상태로 변경
        event.adminUpdateStatus(EventStatus.DELETED)
        eventRepository.save(event)
    }
}
