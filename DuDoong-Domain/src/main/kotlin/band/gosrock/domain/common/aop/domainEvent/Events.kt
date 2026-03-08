package band.gosrock.domain.common.aop.domainEvent

import org.springframework.context.ApplicationEventPublisher

object Events {
    private val publisherLocal = ThreadLocal<ApplicationEventPublisher>()

    @JvmStatic
    fun raise(event: DomainEvent?) {
        if (event == null) return
        publisherLocal.get()?.publishEvent(event)
    }

    @JvmStatic
    fun setPublisher(publisher: ApplicationEventPublisher) {
        publisherLocal.set(publisher)
    }

    @JvmStatic
    fun reset() {
        publisherLocal.remove()
    }
}
