package band.gosrock.common.annotation

import band.gosrock.common.interfaces.SwaggerExampleExceptions
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiErrorExceptionsExample(
    val value: KClass<out SwaggerExampleExceptions>,
)
