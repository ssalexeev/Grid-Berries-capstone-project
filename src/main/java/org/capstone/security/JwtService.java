package org.capstone.security;


import lombok.RequiredArgsConstructor;
import org.capstone.domain.User;
import org.capstone.service.UserService;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
public class JwtService {
    private final String issuer;

    private final Duration ttl;

    private final JwtEncoder jwtEncoder;
    private final UserService userService;

    public String generateToken(final String email){
        User user = userService.getByEmail(email);
        final var claimsSet = JwtClaimsSet.builder()
                .subject(email)
                .issuer(issuer)
                .claim("user_id", user.getId())
                .claim("authority", user.getRole().toString())
                .expiresAt(Instant.now().plus(ttl))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claimsSet)).getTokenValue();
    }
}