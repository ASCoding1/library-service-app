package com.example.libraryservice.user;

import com.example.libraryservice.user.auth.AuthenticationRequest;
import com.example.libraryservice.user.auth.RegisterRequest;
import com.example.libraryservice.user.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    public void testRegister() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("John")
                .surname("Doe")
                .email("johndoe@example.com")
                .password("password123")
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        User savedUser = userRepository.findByEmail("johndoe@example.com").orElse(null);
        assertNotNull(savedUser);
        assertEquals("John", savedUser.getName());
        assertEquals("Doe", savedUser.getSurname());
        assertEquals("johndoe@example.com", savedUser.getEmail());
        assertTrue(passwordEncoder.matches("password123", savedUser.getPassword()));
    }
    @Test
    public void testAuthenticate() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Example");
        registerRequest.setSurname("Examplee");
        registerRequest.setEmail("example@example.com");
        registerRequest.setPassword("test123");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setEmail("example@example.com");
        authenticationRequest.setPassword("test123");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authenticationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }
}

