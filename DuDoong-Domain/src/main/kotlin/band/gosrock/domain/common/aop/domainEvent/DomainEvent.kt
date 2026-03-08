package band.gosrock.domain.common.aop.domainEvent

import java.time.LocalDateTime

open class DomainEvent {
    val publishAt: LocalDateTime = LocalDateTime.now()
}
