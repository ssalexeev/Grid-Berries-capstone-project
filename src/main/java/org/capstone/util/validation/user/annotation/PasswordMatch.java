package org.capstone.util.validation.user.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.capstone.util.validation.user.validator.PasswordMatchValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchValidator.class)
@Documented
public @interface PasswordMatch {
    String message() default "Passwords doesn't match";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
