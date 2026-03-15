package band.gosrock.api.common.aop.hostPartner

import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import org.aspectj.lang.ProceedingJoinPoint
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** 호스트 정보를 트랜잭션 안에서 조회하기 위해서 만든 클래스입니다. 트랜잭션 내에서 캐시 할수 있으면 좋으니 이렇게 만들었습니다. - 이찬진 */
@Component
internal class HostPartnerEventTransaction(
    private val eventAdaptor: EventAdaptor,
    private val hostAdaptor: HostAdaptor
) : HostPartnerCallTransaction {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    override fun proceed(eventId: Long, joinPoint: ProceedingJoinPoint): Any? {
        val event = eventAdaptor.findById(eventId)
        val host = hostAdaptor.findById(event.hostId!!)
        host.validatePartnerHost()
        return joinPoint.proceed()
    }
}
