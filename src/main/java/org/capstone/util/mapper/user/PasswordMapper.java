package org.capstone.util.mapper.user;

import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordMapper {
    private final PasswordEncoder passwordEncoder;


    @Named("encodePassword")
    public String encodePassword(String rawPassword){
        return passwordEncoder.encode(rawPassword);
    }
}
