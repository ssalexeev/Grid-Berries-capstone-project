package org.capstone.controller;

import org.capstone.controller.user.domain.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    void getUserProfile_ReturnsNoContentFromPlaceholder() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk());
    }
}
