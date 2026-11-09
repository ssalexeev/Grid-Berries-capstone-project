package org.capstone.dto.in;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.capstone.domain.enums.Role;
import org.capstone.util.validation.user.annotation.PasswordMatch;

@Getter
@Setter
@Builder
@PasswordMatch
public class UserRegistrationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be less than 150 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Password must contain uppercase, lowercase letters and digits"
    )
    private String password;

    private String passwordConfirm;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean passwordsIsMatch = false;

    @Builder.Default
    private Role role = Role.BUYER;
}
