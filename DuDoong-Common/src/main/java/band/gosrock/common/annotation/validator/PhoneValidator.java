package band.gosrock.common.annotation.validator;

import band.gosrock.common.annotation.Phone;
import javax.validation.*;

public class PhoneValidator implements ConstraintValidator<Phone, String>{
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) { // 2
        if (value == null) {
            return false;
        }

        return value.matches("^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$");
    }
}
