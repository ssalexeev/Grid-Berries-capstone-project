package org.capstone.controller.user.auth;


import lombok.RequiredArgsConstructor;
import org.capstone.domain.User;
import org.capstone.dto.request.UserAuthRequest;
import org.capstone.dto.request.UserRegistrationRequest;
import org.capstone.dto.response.user.UserTokenAuthenticationResponse;
import org.capstone.security.AuthenticationService;
import org.capstone.service.UserService;
import org.capstone.util.mapper.user.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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

    //todo finish implementing of JWT auth logic with refresh tokens table and other parts

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody @Validated UserRegistrationRequest registrationRequest){
        User user = userMapper.toEntity(registrationRequest);
        userService.registerUser(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Account created");
    }

    @PostMapping("/login")
    public ResponseEntity<UserTokenAuthenticationResponse> login(@RequestBody @Validated UserAuthRequest authRequest){
        UserTokenAuthenticationResponse authenticate = authenticationService.authenticate(authRequest);
        return ResponseEntity.ok(authenticate);
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshToken(@RequestBody @Validated UserRegistrationRequest registrationRequest){
        return null;
    }
    @PostMapping("/logout")
    public ResponseEntity<String> logout(){
        return null;
    }
}

