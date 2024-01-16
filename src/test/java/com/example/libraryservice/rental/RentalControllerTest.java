package com.example.libraryservice.rental;

import com.example.libraryservice.book.BookRepository;
import com.example.libraryservice.book.model.Book;
import com.example.libraryservice.common.enums.Role;
import com.example.libraryservice.monitoring_logs.LogMessage;
import com.example.libraryservice.rabbit.service.RabbitMqService;
import com.example.libraryservice.rental.command.CreateRentalCommand;
import com.example.libraryservice.rental.model.Rental;
import com.example.libraryservice.user.UserRepository;
import com.example.libraryservice.user.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RentalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RabbitMqService rabbitMqService;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDetailsService userDetailsService;

    private String authToken;

    private Book book;

    @BeforeEach
    void setUp() {

        User user = new User();
        user.setEmail("test@example.com");
        user.setRole(Role.CUSTOMER);
        user.setSubscriptions(Set.of());
        userRepository.save(user);

        book = new Book();
        book.setTitle("Sample Book");
        book.setBlocked(false);
        bookRepository.save(book);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities());

        authToken = "Bearer " + generateToken(claims, userDetails);
        SecurityContextHolder.clearContext();

    }

    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 24))
                .signWith(SignatureAlgorithm.HS256, "KV9dQWprVwIeeUbP/tR+xp3FpxM0+jU7lhputCfPQdkwnrBe/jGLnEdXgH8y2AudArdd0vIZJ7BrWk8eFDMNjWofSEiJ0d/vR8jdBCUg2wNFZ79s8K60jHvIurz8CaY/BZJ3NAGDAJ66+TmSVc9FgZDcKc4gAD4Ywd7qGit3xiC7EsGkzY3YatB7TjMoUit4ScgqZCTarYTW8Wq8+CKDedDrnP6qfugfUjM8mXRq6RH4xR+Jj+EsFRpS4GEDAoLoV2ySGy70QHd3lw/M1JBBMoo5ql82EajxfFZE3l5s0GyHOK5XUr3e7BhwBh2DqxkTAhYKg9WImkLUOwi5badD69YIucKVxTHqOgkw4HBAL3g=")
                .compact();
    }

    @Test
    public void testRentBook() throws Exception {
        Book book = new Book();
        book.setTitle("Sample Book");
        book.setBlocked(false);
        bookRepository.save(book);

        CreateRentalCommand command = new CreateRentalCommand();
        LocalDate fromDate = LocalDate.now().plusDays(1);
        LocalDate toDate = fromDate.plusDays(7);
        command.setFromDate(fromDate);
        command.setToDate(toDate);
        command.setBookId(book.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/rentals")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isCreated());

        verify(rabbitMqService, times(1)).send(any(LogMessage.class), anyString());
    }

    @Test
    public void testReturnBook() throws Exception {
        User user = userRepository.findByEmail("test@example.com").orElse(null);
        assertNotNull(user);

        Rental rental = new Rental();
        rental.setUser(user);
        rental.setReturned(false);
        rental.setFromDate(LocalDate.now().minusDays(5));
        rental.setToDate(LocalDate.now().plusDays(5));
        rentalRepository.save(rental);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/rentals/{rentalId}/return", rental.getId())
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Rental updatedRental = rentalRepository.findById(rental.getId()).orElse(null);
        assertNotNull(updatedRental);
        assertTrue(updatedRental.isReturned());
        verify(rabbitMqService, times(1)).send(any(LogMessage.class), anyString());
    }

}
