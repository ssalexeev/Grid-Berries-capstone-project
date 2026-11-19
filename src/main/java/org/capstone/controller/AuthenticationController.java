package org.capstone.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.capstone.domain.User;
import org.capstone.dto.request.RefreshTokenRequest;
import org.capstone.dto.request.UserAuthRequest;
import org.capstone.dto.request.UserRegistrationRequest;
import org.capstone.dto.response.user.UserTokenAuthenticationResponse;
import org.capstone.security.AuthenticationService;
import org.capstone.service.UserService;
import org.capstone.util.mapper.user.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final UserService userService;
    private final UserMapper userMapper;
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody @Valid UserRegistrationRequest registrationRequest){
        User user = userMapper.toEntity(registrationRequest);
        userService.registerUser(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Account created");
    }

    @PostMapping("/login")
    public ResponseEntity<UserTokenAuthenticationResponse> login(@RequestBody @Valid UserAuthRequest authRequest){
        UserTokenAuthenticationResponse authenticate = authenticationService.authenticate(authRequest);
        return ResponseEntity.ok(authenticate);
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserTokenAuthenticationResponse> refreshToken(
            @RequestBody @Valid RefreshTokenRequest refreshTokenRequest) {
        UserTokenAuthenticationResponse response =
                authenticationService.refreshToken(refreshTokenRequest.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshTokenRequest logoutRequest) {
        User user = authenticationService.getUserFromAuthenticationContext();
        authenticationService.logout(logoutRequest.refreshToken(), user.getId());

        return ResponseEntity.noContent().build();
    }
}

