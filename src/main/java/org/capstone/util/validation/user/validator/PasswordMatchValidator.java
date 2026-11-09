package org.capstone.util.validation.user.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.capstone.dto.in.UserRegistrationRequest;
import org.capstone.util.validation.user.annotation.PasswordMatch;
import org.springframework.stereotype.Component;

@Component
public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, UserRegistrationRequest> {
    @Override
    public boolean isValid(UserRegistrationRequest userRequest, ConstraintValidatorContext context) {
        String password = userRequest.getPassword();
        String passwordConfirm = userRequest.getPasswordConfirm();

        if (password == null || passwordConfirm == null) {
            return true;
        }

        if (!password.equals(passwordConfirm)){
            // Изменяем дефолтной контекст, чтобы в GlobalErrors вместо error.getObjectName() возвращать "fieldName": "passwordConfirm"
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Passwords do not match")
                    .addPropertyNode("passwordConfirm")
                    .addConstraintViolation();
            return false;
        }
        userRequest.setPasswordsIsMatch(true);
        return true;
    }
}
