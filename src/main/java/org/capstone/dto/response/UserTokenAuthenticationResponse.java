package org.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserTokenAuthenticationResponse {
    private String accessToken;

    //todo add refresh tokens
    //private String refreshToken;
    //private String tokenType;
}
