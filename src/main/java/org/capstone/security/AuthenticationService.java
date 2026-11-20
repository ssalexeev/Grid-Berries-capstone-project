package org.capstone.security;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.capstone.domain.User;
import org.capstone.domain.enums.Role;
import org.capstone.dto.request.UserAuthRequest;
import org.capstone.dto.response.user.UserTokenAuthenticationResponse;
import org.capstone.repository.jpa.RefreshTokenRepository;
import org.capstone.security.entity.RefreshToken;
import org.capstone.security.exception.SellerRoleAuthenticationException;
import org.capstone.security.exception.UserAuthenticationException;
import org.capstone.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public UserTokenAuthenticationResponse refreshToken(String requestRefreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (storedToken.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(storedToken.getUserId());
            throw new BadCredentialsException("Refresh token was already used or revoked");
        }

        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new BadCredentialsException("Refresh token has expired. Please log in again");
        }

        User user = userService.getUserById(storedToken.getUserId());
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new DisabledException("User account is disabled");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return jwtService.generateTokenPair(user);
    }

    @Transactional
    public UserTokenAuthenticationResponse authenticate(final UserAuthRequest request) {
        final var authToken = UsernamePasswordAuthenticationToken
                .unauthenticated(request.getEmail(), request.getPassword());

        authenticationManager.authenticate(authToken);

        User user = userService.getUserByEmail(request.getEmail());
        return jwtService.generateTokenPair(user);
    }

    @Transactional
    public void logout(String refreshToken, Long currentUserId) {

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new EntityNotFoundException("Refresh token not found"));

        if (!storedToken.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Token does not belong to the current user");
        }

        if (storedToken.isRevoked()) {
            return;
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);
    }

    public Long getSellerIdFromAuthenticationContext() {
        Jwt jwt = getJwtFromContext();

        String authorityClaim = jwt.getClaimAsString("authority");
        if (authorityClaim == null || Role.valueOf(authorityClaim) != Role.SELLER) {
            throw new SellerRoleAuthenticationException("User hasn't Seller Role");
        }

        Long sellerId = jwt.getClaim("seller_id");
        if (sellerId == null) {
            throw new SellerRoleAuthenticationException("Seller profile ID missing from token");
        }
        return sellerId;
    }

    public User getUserFromAuthenticationContext() {
        Jwt jwt = getJwtFromContext();

        Long userId = jwt.getClaim("user_id");
        if (userId == null) {
            throw new UserAuthenticationException("User ID missing from token");
        }
        return userService.getUserById(userId);
    }

    private Jwt getJwtFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UserAuthenticationException("User is unauthenticated or token is incorrect");
        }
        return jwt;
    }
}
