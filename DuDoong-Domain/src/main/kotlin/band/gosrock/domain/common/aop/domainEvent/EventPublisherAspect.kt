package band.gosrock.domain.common.aop.domainEvent

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationEventPublisherAware
import org.springframework.stereotype.Component

@Aspect
@Component
@ConditionalOnExpression("\${ableDomainEvent:true}")
class EventPublisherAspect : ApplicationEventPublisherAware {

    private lateinit var publisher: ApplicationEventPublisher
    private val appliedLocal = ThreadLocal<Boolean>()

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    @Throws(Throwable::class)
    fun handleEvent(joinPoint: ProceedingJoinPoint): Any? {
        val appliedValue = appliedLocal.get()
        val nested = appliedValue != null && appliedValue

        if (!nested) {
            appliedLocal.set(true)
            Events.setPublisher(publisher)
        }

        return try {
            joinPoint.proceed()
        } finally {
            if (!nested) {
                Events.reset()
                appliedLocal.remove()
            }
        }
    }

    override fun setApplicationEventPublisher(eventPublisher: ApplicationEventPublisher) {
        this.publisher = eventPublisher
    }
}
