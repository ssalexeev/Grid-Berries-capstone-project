package org.capstone.util.validation.user.annotation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.capstone.util.validation.user.validator.UserEmailNotOccupyValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UserEmailNotOccupyValidator.class)
@Documented
public @interface EmailNotOccupy {
    String message() default "Email already uses";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}