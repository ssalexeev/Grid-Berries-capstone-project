package org.capstone.security;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.enums.Role;
import org.capstone.dto.request.UserAuthRequest;
import org.capstone.dto.response.UserTokenAuthenticationResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public UserTokenAuthenticationResponse authenticate(final UserAuthRequest request) {
        final var authToken = UsernamePasswordAuthenticationToken
                .unauthenticated(request.getEmail(), request.getPassword());

        final var authentication = authenticationManager.authenticate(authToken);

        final var accessToken = jwtService.generateToken(request.getEmail());

        return UserTokenAuthenticationResponse.builder()
                .accessToken(accessToken)
                .build();
    }

    public Long getCurrentSellerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UserAuthenticationException("User is unauthenticated or token is incorrect");
        }

        Role authority = jwt.getClaim("authority");

        if (authority == null) {
            throw new UserAuthenticationException("JWT doesn't contain 'authority' claim");
        }
        if (authority != Role.SELLER) {
            throw new SellerRoleAuthenticationException("User hasn't Seller Role");
        }

        return jwt.getClaim("user_id");
    }
}
