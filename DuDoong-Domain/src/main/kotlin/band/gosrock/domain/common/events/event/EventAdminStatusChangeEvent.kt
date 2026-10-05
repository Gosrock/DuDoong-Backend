package band.gosrock.domain.common.events.event

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.event.domain.EventStatus

/**
 * 운영 어드민이 공연 상태를 바꿈 (운영 삭제 `DELETE /internal-api/v1/events/{id}`, 상태 변경 `PATCH .../status`, #719).
 * 같은 트랜잭션(BEFORE_COMMIT)에서 공연 삭제·비공개 전환이면 대기 선물을 취소한다 (DEC-026 #9). 슬랙 알림 대상 아님
 */
data class EventAdminStatusChangeEvent(
    val eventId: Long,
    val status: EventStatus,
) : DomainEvent()
