package band.gosrock.domain.common.events.user

import band.gosrock.domain.common.aop.domainEvent.DomainEvent

class UserRegisterEvent(val userId: Long?) : DomainEvent() {
    override fun toString(): String = "UserRegisterEvent(userId=$userId)"
}
