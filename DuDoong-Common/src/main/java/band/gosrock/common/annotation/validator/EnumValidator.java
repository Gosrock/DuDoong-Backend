package band.gosrock.common.annotation.validator;


import band.gosrock.common.annotation.Enum;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

public class EnumValidator implements ConstraintValidator<Enum, java.lang.Enum> {
    @Override
    public boolean isValid(java.lang.Enum value, ConstraintValidatorContext context) {
        boolean result = false;
        try {
            // 리플렉션을 이용한 Enum 클래스 가져오기
            Class<?> reflectionEnumClass = new BeanWrapperImpl(value).getWrappedClass();
            Object[] enumValues = reflectionEnumClass.getEnumConstants();
            if (enumValues != null) {
                for (Object enumValue : enumValues) {
                    if (value == enumValue) {
                        result = true;
                        break;
                    }
                }
            }
            return result;
        } catch (Exception e) {
            return false;
        }
    }
}
