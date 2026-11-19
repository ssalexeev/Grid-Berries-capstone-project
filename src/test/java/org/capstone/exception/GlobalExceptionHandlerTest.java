package org.capstone.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.capstone.controller.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.DummyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        GlobalExceptionHandlerTest.DummyController.class
})
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Validated
    @RestController
    @RequestMapping("/api/v1/test-exceptions")
    static class DummyController {

        record TestRequest(
                @NotBlank(message = "Title must not be blank") String title,
                @Min(value = 1, message = "Price must be at least 1") Integer price
        ) {}

        @PostMapping("/body-validation")
        void bodyValidation(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/param-validation")
        void paramValidation(
                @RequestParam @Min(value = 1, message = "Id must be at least 1") Long id) {
        }

        @GetMapping("/uncaught")
        void uncaught() {
            throw new RuntimeException("Sensitive database details");
        }
    }

    @Test
    void handleMethodArgumentNotValid_ReturnsRfc7807ProblemDetail() throws Exception {
        String invalidPayload = """
                {
                  "title": "",
                  "price": 0
                }
                """;

        mockMvc.perform(post("/api/v1/test-exceptions/body-validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.instance").value("/api/v1/test-exceptions/body-validation"))
                .andExpect(jsonPath("$.errors.title").value("Title must not be blank"))
                .andExpect(jsonPath("$.errors.price").value("Price must be at least 1"));
    }

    @Test
    void handleConstraintViolationException_ReturnsRfc7807ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/test-exceptions/param-validation")
                        .param("id", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Constraint Violation"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed for one or more parameters"))
                .andExpect(jsonPath("$.instance").value("/api/v1/test-exceptions/param-validation"))
                .andExpect(jsonPath("$.errors['paramValidation.id']").value("Id must be at least 1"));
    }

    @Test
    void handleHttpRequestMethodNotSupported_Returns405ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/test-exceptions/body-validation"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void handleAllUncaughtException_Returns500ProblemDetailWithoutLeakingMessage() throws Exception {
        mockMvc.perform(get("/api/v1/test-exceptions/uncaught"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected internal server error occurred"))
                .andExpect(jsonPath("$.instance").value("/api/v1/test-exceptions/uncaught"));
    }
}