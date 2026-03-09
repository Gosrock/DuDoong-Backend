package band.gosrock.domain.common.events.user

import band.gosrock.domain.common.aop.domainEvent.DomainEvent

class UserRegisterEvent private constructor(val userId: Long?) : DomainEvent() {
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var userId: Long? = null
        fun userId(v: Long?) = apply { userId = v }
        fun build() = UserRegisterEvent(userId)
    }

    override fun toString(): String = "UserRegisterEvent(userId=$userId)"
}
