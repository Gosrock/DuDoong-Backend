package band.gosrock.common.annotation;

import band.gosrock.common.annotation.validator.PhoneValidator;
import org.springframework.stereotype.Component;

import javax.validation.Constraint;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class) // 3
public @interface Phone {
    String message() default "";
    Class[] groups() default {};
    Class[] payload() default {};
}
