package org.capstone.security;


import lombok.RequiredArgsConstructor;
import org.capstone.domain.SellerProfile;
import org.capstone.domain.User;
import org.capstone.domain.enums.Role;
import org.capstone.dto.response.user.UserTokenAuthenticationResponse;
import org.capstone.repository.jpa.RefreshTokenRepository;
import org.capstone.security.entity.RefreshToken;
import org.capstone.service.SellerProfileService;
import org.capstone.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${spring.application.name}")
    private String issuer;

    @Value("${jwt.ttl}")
    private  Duration ttl;
    @Value("${jwt.refresh-token.expiration}")
    private Duration refreshTokenTtl;

    private final JwtEncoder jwtEncoder;
    private final UserService userService;
    private final SellerProfileService sellerProfileService;
    private final RefreshTokenRepository refreshTokenRepository;


    public UserTokenAuthenticationResponse generateTokenPair(final User user) {

        String accessToken = buildAccessToken(user);
        String refreshToken = createAndSaveRefreshToken(user.getId());

        return new UserTokenAuthenticationResponse(accessToken, refreshToken);
    }

    private String buildAccessToken(User user) {
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .subject(user.getEmail())
                .issuer(issuer)
                .claim("user_id", user.getId())
                .claim("authority", user.getRole().toString())
                .expiresAt(Instant.now().plus(ttl));

        if (user.getRole() == Role.SELLER) {
            Long sellerProfileId = sellerProfileService.getProfileByUserId(user.getId()).getId();
            claimsBuilder.claim("seller_id", sellerProfileId);
        }

        return jwtEncoder.encode(JwtEncoderParameters.from(claimsBuilder.build())).getTokenValue();
    }

    public String createAndSaveRefreshToken(Long userId) {
        String tokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(tokenValue);
        refreshToken.setExpiryDate(LocalDateTime.now().plusMinutes(refreshTokenTtl.toMinutes()));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }
}