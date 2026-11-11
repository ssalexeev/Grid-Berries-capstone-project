package org.capstone.controller.user.domain;


import lombok.RequiredArgsConstructor;
import org.capstone.dto.response.UserProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getUserProfile(){
        return null;
    }
}
