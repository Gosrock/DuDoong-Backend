package band.gosrock.common.annotation

import band.gosrock.common.validator.EnumValidator
import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

/** RequestBody 의 Enum 검증을 위한 어노테이션 입니다 */
@Constraint(validatedBy = [EnumValidator::class])
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Enum(
    val message: String = "Invalid Enum Value.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
