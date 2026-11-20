package org.capstone.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserTokenAuthenticationResponse {
    private String accessToken;
    //todo add refresh tokens
    private String refreshToken;
    //private String tokenType;
}
