package band.gosrock.common.annotation;


import band.gosrock.common.annotation.validator.PhoneValidator;
import java.lang.annotation.*;
import javax.validation.Constraint;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
public @interface Phone {
    String message() default "";

    Class[] groups() default {};

    Class[] payload() default {};
}
