package com.example.libraryservice.subscription;

import com.example.libraryservice.common.enums.Role;
import com.example.libraryservice.rabbit.service.RabbitMqService;
import com.example.libraryservice.subscription.command.CreateSubscriptionCommand;
import com.example.libraryservice.subscription.model.Subscription;
import com.example.libraryservice.subscription.model.SubscriptionDto;
import com.example.libraryservice.user.UserRepository;
import com.example.libraryservice.user.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @MockBean
    private RabbitMqService rabbitMqService;


    private String authToken;

    @BeforeEach
    public void init() {
        Subscription subscription = new Subscription();
        subscription.setCreationDate(LocalDate.now().plusDays(1));
        subscription.setCategoryName("HORROR");

        User user = new User();
        user.setEmail("test@example.com");
        user.setRole(Role.CUSTOMER);
        user.setSubscriptions(Set.of(subscription));
        userRepository.save(user);

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
                .signWith(SignatureAlgorithm.HS256, "   \"KV9dQWprVwIeeUbP/tR+xp3FpxM0+jU7lhputCfPQdkwnrBe/jGLnEdXgH8y2AudArdd0vIZJ7BrWk8eFDMNjWofSEiJ0d/vR8jdBCUg2wNFZ79s8K60jHvIurz8CaY/BZJ3NAGDAJ66+TmSVc9FgZDcKc4gAD4Ywd7qGit3xiC7EsGkzY3YatB7TjMoUit4ScgqZCTarYTW8Wq8+CKDedDrnP6qfugfUjM8mXRq6RH4xR+Jj+EsFRpS4GEDAoLoV2ySGy70QHd3lw/M1JBBMoo5ql82EajxfFZE3l5s0GyHOK5XUr3e7BhwBh2DqxkTAhYKg9WImkLUOwi5badD69YIucKVxTHqOgkw4HBAL3g=")
                .compact();
    }

    @Test
    public void testSaveSubscription() throws Exception {
        LocalDate localDate = LocalDate.now();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/sub")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryName\": \"TestCategory\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.categoryName").value("TestCategory"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.creationDate").value(localDate.toString()))
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void testFindAllSubscriptions() throws Exception {
        LocalDate localDate = LocalDate.now();
        Subscription subscription = new Subscription();
        subscription.setId(1);
        subscription.setCategoryName("TestCategory");
        subscription.setCreationDate(LocalDate.now());

        subscriptionRepository.save(subscription);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/sub")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].categoryName").value("TestCategory"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].creationDate").value(localDate.toString()))
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void testSubscribeAndUnsubscribeToCategory() throws Exception {
        LocalDate localDate = LocalDate.now();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/sub")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryName\": \"ADVENTURE\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.categoryName").value("ADVENTURE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.creationDate").value(localDate.toString()))
                .andDo(MockMvcResultHandlers.print());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/sub")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .param("bookCategory", "ADVENTURE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

}
