package band.gosrock.common.annotation

import band.gosrock.common.exception.BaseErrorCode
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiErrorCodeExample(
    val value: KClass<out BaseErrorCode>,
)
