package org.capstone.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.capstone.domain.enums.Role;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserModerationResponse {
    private String email;
    private Role role;
    private Boolean isActive;
    private LocalDateTime createdAt;

}
