package band.gosrock.admin.service

import band.gosrock.admin.model.dto.request.AdminUpdateEventStatusRequest
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.repository.EventRepository
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminUpdateEventStatusUseCase(
    private val eventAdaptor: EventAdaptor,
    private val eventRepository: EventRepository,
) {

    @Transactional
    fun execute(eventId: Long, request: AdminUpdateEventStatusRequest) {
        val event = eventAdaptor.findById(eventId)
        // 어드민은 DELETED 제외 모든 상태로 직접 변경 가능 (밸리데이션 우회)
        require(request.status != EventStatus.DELETED) {
            "DELETED 상태는 DELETE 엔드포인트를 사용하세요."
        }
        event.adminUpdateStatus(request.status)
        eventRepository.save(event)
    }
}
