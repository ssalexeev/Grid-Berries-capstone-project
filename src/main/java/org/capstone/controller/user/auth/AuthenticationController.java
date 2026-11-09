package org.capstone.controller.user.auth;


import lombok.RequiredArgsConstructor;
import org.capstone.dto.in.UserAuthRequest;
import org.capstone.dto.in.UserRegistrationRequest;
import org.capstone.service.UserService;
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

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody @Validated UserRegistrationRequest registrationRequest){
        userService.registerUser(registrationRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Account created");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Validated UserAuthRequest authRequest){
        return null;
    }

    @PostMapping("/login")
    public ResponseEntity<String> refreshToken(@RequestBody @Validated UserRegistrationRequest registrationRequest){
        return null;
    }
    @PostMapping("/login")
    public ResponseEntity<String> logout(@RequestBody @Validated UserRegistrationRequest registrationRequest){
        return null;
    }
}

