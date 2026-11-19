package org.capstone.IT.controller;

import org.capstone.controller.AuthenticationController;
import org.capstone.domain.User;
import org.capstone.dto.request.UserAuthRequest;
import org.capstone.dto.request.UserRegistrationRequest;
import org.capstone.dto.response.user.UserTokenAuthenticationResponse;
import org.junit.jupiter.api.Test;
import org.capstone.security.AuthenticationService;
import org.capstone.service.UserService;
import org.capstone.util.mapper.user.UserMapper;
import org.capstone.util.validation.user.validator.PasswordMatchValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PasswordMatchValidator.class)
class AuthenticationControllerWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @Test
    void register_ReturnsCreatedAndRegistersMappedUser() throws Exception {
        User user = User.builder().email("buyer@example.com").build();
        when(userMapper.toEntity(any(UserRegistrationRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "buyer@example.com",
                                  "password": "Password123",
                                  "passwordConfirm": "Password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string("Account created"));

        verify(userMapper).toEntity(any(UserRegistrationRequest.class));
        verify(userService).registerUser(user);
    }

    @Test
    void login_ReturnsSerializedTokens() throws Exception {
        when(authenticationService.authenticate(any(UserAuthRequest.class)))
                .thenReturn(UserTokenAuthenticationResponse.builder()
                        .accessToken("access-token")
                        .refreshToken("refresh-token")
                        .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "buyer@example.com",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

        verify(authenticationService).authenticate(any(UserAuthRequest.class));
    }

    @Test
    void refresh_ReturnsSerializedTokensAndPassesRefreshToken() throws Exception {
        when(authenticationService.refreshToken("old-refresh-token"))
                .thenReturn(UserTokenAuthenticationResponse.builder()
                        .accessToken("new-access-token")
                        .refreshToken("new-refresh-token")
                        .build());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "old-refresh-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));

        verify(authenticationService).refreshToken("old-refresh-token");
    }

    @Test
    void logout_ReturnsNoContentAndPassesAuthenticatedUserId() throws Exception {
        when(authenticationService.getUserFromAuthenticationContext())
                .thenReturn(User.builder().id(42L).build());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "refresh-token"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(authenticationService).logout("refresh-token", 42L);
    }

    @Test
    void login_WithInvalidEmailReturnsValidationErrorWithoutAuthenticating() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.validationErrors.email").value("Invalid email format"));

        verify(authenticationService, never()).authenticate(any(UserAuthRequest.class));
    }
}
