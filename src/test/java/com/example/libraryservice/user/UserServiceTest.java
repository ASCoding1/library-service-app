package com.example.libraryservice.user;

import com.example.libraryservice.common.enums.Role;
import com.example.libraryservice.security.jwt.JwtService;
import com.example.libraryservice.user.auth.AuthenticationRequest;
import com.example.libraryservice.user.auth.AuthenticationResponse;
import com.example.libraryservice.user.auth.RegisterRequest;
import com.example.libraryservice.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testRegister() {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("John")
                .surname("Doe")
                .email("johndoe@example.com")
                .password("password123")
                .build();

        User mockUser = User.builder()
                .name("John")
                .surname("Doe")
                .email("johndoe@example.com")
                .password("hashedPassword")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(jwtService.generateToken(any(User.class))).thenReturn("generatedToken");

        AuthenticationResponse response = userService.register(registerRequest);

        assertEquals("generatedToken", response.getToken());

        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("password123");
        verify(jwtService, times(1)).generateToken(any(User.class));
    }

    @Test
    public void testAuthenticate() {
        AuthenticationRequest authRequest = new AuthenticationRequest("johndoe@example.com", "password123");

        User mockUser = User.builder()
                .name("John")
                .surname("Doe")
                .email("johndoe@example.com")
                .password("hashedPassword")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.findByEmail("johndoe@example.com")).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(any(User.class))).thenReturn("generatedToken");

        userService.authenticate(authRequest);

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateToken(any(User.class));
    }

    @Test
    public void testAuthenticate_UserNotFound() {
        AuthenticationRequest authRequest = new AuthenticationRequest("nonexistent@example.com", "password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        try {
            userService.authenticate(authRequest);
        } catch (UsernameNotFoundException e) {
            assertEquals("USER_NOT_FOUND", e.getMessage());
        }
    }
}