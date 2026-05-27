package com.example.shipmenttrackingportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.shipmenttrackingportal.dto.LoginRequest;
import com.example.shipmenttrackingportal.dto.RegisterRequest;
import com.example.shipmenttrackingportal.enums.Role;
import com.example.shipmenttrackingportal.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration test for the authentication flow.
 *
 * @SpringBootTest loads the full application context.
 * @AutoConfigureMockMvc provides MockMvc without starting an actual HTTP server.
 * @ActiveProfiles("test") activates application-test.properties (H2 in-memory DB).
 *
 * Tests the complete register → login → JWT token flow end-to-end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Full auth flow: register SHIPPER → login → receive JWT token")
    void registerAndLogin_Shipper_ReceivesJwtToken() throws Exception {
        // --- Step 1: Register ---
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFullName("Test Shipper");
        registerRequest.setEmail("test.shipper@integration.com");
        registerRequest.setPassword("TestPass@123");
        registerRequest.setRole(Role.SHIPPER);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.role").value("SHIPPER"));

        // --- Step 2: Login ---
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test.shipper@integration.com");
        loginRequest.setPassword("TestPass@123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("SHIPPER"))
                .andExpect(jsonPath("$.email").value("test.shipper@integration.com"))
                .andReturn();

        // Verify token is a valid JWT structure (3 dot-separated Base64 segments)
        String responseBody = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Register with duplicate email returns 400 Bad Request")
    void register_DuplicateEmail_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Duplicate User");
        request.setEmail("dup@integration.com");
        request.setPassword("Duplicate@123");
        request.setRole(Role.CARRIER);

        // First registration succeeds
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second registration with same email fails
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("already exists")));
    }

    @Test
    @DisplayName("Login with wrong password returns 401 Unauthorized")
    void login_WrongPassword_Returns401() throws Exception {
        // Register first
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFullName("Wrong Pass User");
        registerRequest.setEmail("wrongpass@integration.com");
        registerRequest.setPassword("Correct@123");
        registerRequest.setRole(Role.CARRIER);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Try login with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("wrongpass@integration.com");
        loginRequest.setPassword("Wrong@Password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Register with blank fields returns 400 with validation details")
    void register_BlankFields_Returns400WithDetails() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("");
        request.setEmail("not-an-email");
        request.setPassword("123");
        request.setRole(null);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.password").exists());
    }
}
