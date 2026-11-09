package org.capstone.util.validation.user.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.capstone.repository.jpa.UserRepository;
import org.capstone.util.validation.user.annotation.EmailNotOccupy;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class UserEmailNotOccupyValidator implements ConstraintValidator<EmailNotOccupy, String> {
    private final UserRepository userRepository;

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        return  userRepository.existsByEmailIgnoreCase(email);
    }
}
