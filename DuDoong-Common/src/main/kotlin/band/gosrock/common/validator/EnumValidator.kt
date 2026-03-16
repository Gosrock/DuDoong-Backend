package band.gosrock.common.validator

import band.gosrock.common.annotation.Enum as EnumAnnotation
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class EnumValidator : ConstraintValidator<EnumAnnotation, kotlin.Enum<*>> {
    override fun isValid(value: kotlin.Enum<*>?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return false
        val reflectionEnumClass = value.declaringJavaClass
        return reflectionEnumClass.enumConstants.contains(value)
    }
}
