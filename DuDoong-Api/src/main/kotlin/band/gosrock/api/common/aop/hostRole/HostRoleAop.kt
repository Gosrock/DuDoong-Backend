package band.gosrock.api.common.aop.hostRole

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component

/**
 * 호스트 관리자 인가를 위한 aop 입니다 메소드 레벨에서 작동하며 권한 정보를 어노테이션으로 받고 eventId를 인자에서 찾아와 호스트 정보를 불러온뒤 권한 검증을 합니다.
 */
@Aspect
@Component
@ConditionalOnExpression("\${ableHostRoleAop:true}")
internal class HostRoleAop(
    private val hostCallTransactionFactory: HostCallTransactionFactory
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * master 호스트의 마스터 manager 호스트의 수정,조회 ( 호스트유저도메인의 슈퍼 호스트 ) guest 호스트의 조회권한 (호스트유저도메인의 호스트 )
     *
     * @see band.gosrock.domain.domains.host.domain.HostRole
     */
    @Around("@annotation(band.gosrock.api.common.aop.hostRole.HostRolesAllowed)")
    @Throws(Throwable::class)
    fun aop(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val annotation = method.getAnnotation(HostRolesAllowed::class.java)
        val hostQualification = annotation.role
        val findHostFrom = annotation.findHostFrom
        val identifier = findHostFrom.identifier

        val parameterNames = signature.parameterNames
        val args = joinPoint.args

        val userId = getId(parameterNames, args, "userId")
        val id = getId(parameterNames, args, identifier)

        return hostCallTransactionFactory
            .getCallTransaction(findHostFrom, annotation.applyTransaction)
            .proceed(userId, id, hostQualification, joinPoint)
    }

    fun getId(parameterNames: Array<String>, args: Array<Any?>, paramName: String): Long {
        for (i in parameterNames.indices) {
            if (parameterNames[i] == paramName) {
                return args[i] as Long
            }
        }
        throw IllegalArgumentException()
    }
}
