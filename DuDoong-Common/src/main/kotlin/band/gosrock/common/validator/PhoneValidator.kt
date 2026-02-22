package band.gosrock.common.validator

import band.gosrock.common.annotation.Phone
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class PhoneValidator : ConstraintValidator<Phone, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return false
        return value.matches(Regex("^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$"))
    }
}
