package org.capstone.security;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.User;
import org.capstone.domain.enums.Role;
import org.capstone.dto.request.UserAuthRequest;
import org.capstone.dto.response.user.UserTokenAuthenticationResponse;
import org.capstone.security.exception.SellerRoleAuthenticationException;
import org.capstone.security.exception.UserAuthenticationException;
import org.capstone.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public UserTokenAuthenticationResponse authenticate(final UserAuthRequest request) {
        final var authToken = UsernamePasswordAuthenticationToken
                .unauthenticated(request.getEmail(), request.getPassword());

        final var authentication = authenticationManager.authenticate(authToken);

        final var accessToken = jwtService.generateToken(request.getEmail());

        return UserTokenAuthenticationResponse.builder()
                .accessToken(accessToken)
                .build();
    }

    public Long getSellerIdFromAuthenticationContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UserAuthenticationException("User is unauthenticated or token is incorrect");
        }

        Role authority = Role.valueOf(jwt.getClaim("authority"));

        if (authority != Role.SELLER) {
            throw new SellerRoleAuthenticationException("User hasn't Seller Role");
        }

        return jwt.getClaim("seller_id");
    }

    public User getUserFromAuthenticationContext(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UserAuthenticationException("User is unauthenticated or token is incorrect");
        }

        return userService.getUserById(jwt.getClaim("user_id"));
    }
}
