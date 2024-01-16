package com.example.libraryservice.rabbit_transfer_tests;

import com.example.libraryservice.book.command.CreateBookCommand;
import com.example.libraryservice.common.enums.Role;
import com.example.libraryservice.monitoring_logs.LogMessage;
import com.example.libraryservice.subscription.SubscriptionRepository;
import com.example.libraryservice.subscription.model.Subscription;
import com.example.libraryservice.user.UserRepository;
import com.example.libraryservice.user.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RabbitMqTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final MessageConverter jsonMessageConverter = new Jackson2JsonMessageConverter();

    private static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.12.1-management")
            .withExposedPorts(5672, 15672)
            .withNetworkAliases("shared-rabbitmq")
            .withNetwork(Network.SHARED)
            .withQueue("library-queue")
            .withQueue("logging-queue");

    @BeforeAll
    public static void beforeAll() {
        rabbitMQContainer.start();
    }

    @BeforeEach
    public void init() {
        String userEmail = "user997@example.com";
        Optional<User> existingUserOptional = userRepository.findByEmail(userEmail);

        existingUserOptional.ifPresentOrElse(
                existingUser -> {
                    if (existingUser.getSubscriptions() == null) {
                        existingUser.setSubscriptions(new HashSet<>());
                    }
                },
                () -> {
                    User user = new User();
                    user.setRole(Role.CUSTOMER);
                    user.setEmail(userEmail);

                    userRepository.saveAndFlush(user);

                    Subscription subscription = new Subscription();
                    subscription.setCategoryName("ADVENTURE");
                    subscription.setCreationDate(LocalDate.now());
                    subscription.setActive(true);
                    subscription.setUser(user);

                    subscriptionRepository.saveAndFlush(subscription);

                    if (user.getSubscriptions() == null) {
                        user.setSubscriptions(new HashSet<>());
                    }
                    user.getSubscriptions().add(subscription);

                    userRepository.saveAndFlush(user);
                }
        );
    }

    @DynamicPropertySource
    public static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
    }

    //losowa przykładowa metoda by sprawdzić działanie MonitorMethod
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    public void should_Save_Book_And_Check_MonitorMethod_Working() throws Exception {
        CreateBookCommand command = new CreateBookCommand();
        command.setTitle("New Book");
        command.setAuthor("Author");
        command.setCategory("ADVENTURE");
        command.setBlocked(false);

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is(command.getTitle())))
                .andExpect(jsonPath("$.blocked", is(command.isBlocked())))
                .andExpect(jsonPath("$.category", is(command.getCategory())))
                .andExpect(jsonPath("$.author", is(command.getAuthor())));

        Awaitility.await().untilAsserted(() -> {
            Message receivedMessage = rabbitTemplate.receive("logging-queue");
            assertNotNull(receivedMessage);

            LogMessage expectedMessage = new LogMessage();
            expectedMessage.setMethodName("save");
            expectedMessage.setClassName("BookService");
            expectedMessage.setUserEmail("user");

            LogMessage receivedLogMessage = (LogMessage) jsonMessageConverter.fromMessage(receivedMessage);
            assertEquals(expectedMessage.getUserEmail(), receivedLogMessage.getUserEmail());
            assertEquals(expectedMessage.getClassName(), receivedLogMessage.getClassName());
            assertEquals(expectedMessage.getMethodName(), receivedLogMessage.getMethodName());
        });
    }

    @AfterAll
    public static void close() {
        rabbitMQContainer.close();
        rabbitMQContainer.stop();
    }
}


