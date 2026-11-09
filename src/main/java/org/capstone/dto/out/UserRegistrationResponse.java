package org.capstone.dto.out;

import lombok.Builder;
import lombok.Getter;
import org.capstone.domain.enums.Role;

@Getter
@Builder
public class UserRegistrationResponse {
    private String email;
    private Role role;
}
